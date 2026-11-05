package com.stackr.runner.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Every compilation and test runs in a fresh, restricted Docker container. */
@Slf4j
@Service
public class DockerService {
    private final ThreadLocal<Long> deadline = new ThreadLocal<>();
    public void beginBudget(long milliseconds) { deadline.set(System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(milliseconds)); }
    public void endBudget() { deadline.remove(); }

    private static final int OUTPUT_LIMIT = 1024 * 1024;
    @Value("${sandbox.image:eclipse-temurin:21-jdk-alpine}")
    private String image = "eclipse-temurin:21-jdk-alpine";

    public record CompileResult(boolean ok, String errorMessage, Path classDir) {}
    public record ExecutionResult(String stdout, String stderr, int exitCode, int elapsedMs) {}

    public boolean isReady() {
        try {
            return execute(List.of("docker", "image", "inspect", "--format", "{{.Id}}", image), "", 5000).exitCode() == 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (IOException e) { return false; }
    }

    public CompileResult compile(String sourceCode) throws IOException, InterruptedException {
        Path dir = Files.createTempDirectory("stackr-job-");
        String volume = dir.getFileName().toString();
        String container = volume + "-compile";
        boolean ok = false;
        try {
            Files.writeString(dir.resolve("Solution.java"), sourceCode, StandardCharsets.UTF_8);
            Files.setPosixFilePermissions(dir.resolve("Solution.java"), PosixFilePermissions.fromString("rw-r--r--"));
            Files.createDirectory(dir.resolve("classes"));
            Files.setPosixFilePermissions(dir.resolve("classes"), PosixFilePermissions.fromString("rwxrwxrwx"));
            checked(List.of("docker", "volume", "create", volume));
            List<String> args = containerArgs(container, volume, false, 512);
            args.addAll(List.of(image, "javac", "-J-Xmx256m", "-proc:none", "-encoding", "UTF-8",
                    "-d", "/workspace/classes", "/workspace/Solution.java"));
            checked(args);
            checked(List.of("docker", "cp", dir + "/.", container + ":/workspace"));
            ExecutionResult result = execute(List.of("docker", "start", "--attach", container), "", 30000);
            if (result.exitCode() != 0) {
                if (result.exitCode() == 125) throw new IOException("Sandbox image or Docker daemon unavailable");
                return new CompileResult(false, result.stdout() + result.stderr(), null);
            }
            ok = true;
            return new CompileResult(true, null, dir);
        } finally {
            removeContainer(container);
            if (!ok) cleanup(dir);
        }
    }

    public ExecutionResult runTest(Path classDir, String input, int timeLimitMs, int memoryLimitMb)
            throws IOException, InterruptedException {
        if (timeLimitMs < 100 || timeLimitMs > 30000 || memoryLimitMb < 64 || memoryLimitMb > 1024) {
            throw new IllegalArgumentException("Unsupported execution limits");
        }
        String volume = classDir.getFileName().toString();
        String container = volume + "-" + UUID.randomUUID();
        try {
            List<String> args = containerArgs(container, volume, true, memoryLimitMb);
            args.addAll(List.of(image, "java", "-XX:+UseSerialGC", "-XX:ActiveProcessorCount=1",
                    "-Xmx" + Math.max(16, memoryLimitMb / 2) + "m", "-Xss1m",
                    "-cp", "/workspace/classes", "Solution"));
            checked(args);
            // Timing includes Docker attach/start overhead; it is wall time, not CPU time.
            return execute(List.of("docker", "start", "--attach", "--interactive", container),
                    input == null ? "" : input, timeLimitMs);
        } finally {
            removeContainer(container);
        }
    }

    static List<String> containerArgs(String name, String volume, boolean readOnly, int memoryMb) {
        return new ArrayList<>(List.of("docker", "create", "--name", name, "--interactive",
                "--network", "none", "--read-only", "--cap-drop", "ALL",
                "--security-opt", "no-new-privileges:true", "--user", "65534:65534",
                "--cpus", "1", "--pids-limit", "64", "--memory", memoryMb + "m",
                "--memory-swap", memoryMb + "m", "--ulimit", "nofile=128:128",
                "--log-driver", "none", "--tmpfs", "/tmp:rw,noexec,nosuid,size=32m",
                "--mount", "type=volume,src=" + volume + ",dst=/workspace" + (readOnly ? ",readonly" : ""),
                "--workdir", "/tmp"));
    }

    private void checked(List<String> command) throws IOException, InterruptedException {
        ExecutionResult r = execute(command, "", 30000);
        if (r.exitCode() != 0) throw new IOException("Docker operation failed: " + r.stderr() + r.stdout());
    }

    /** Drain stdout, stderr and write stdin concurrently: full pipes must not defeat timeouts. */
    private ExecutionResult execute(List<String> command, String input, long timeoutMs)
            throws IOException, InterruptedException {
        Long end = deadline.get();
        if (end != null) {
            long remaining = TimeUnit.NANOSECONDS.toMillis(end - System.nanoTime());
            if (remaining <= 0) throw new IOException("Total judge time budget exceeded");
            timeoutMs = Math.min(timeoutMs, remaining);
        }
        Process process = new ProcessBuilder(command).start();
        AtomicBoolean overflow = new AtomicBoolean();
        long start = System.nanoTime();
        try (ExecutorService io = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> stdout = io.submit(() -> capture(process.getInputStream(), overflow, process));
            Future<String> stderr = io.submit(() -> capture(process.getErrorStream(), overflow, process));
            Future<?> writer = io.submit(() -> {
                try (OutputStream stream = process.getOutputStream()) {
                    stream.write(input.getBytes(StandardCharsets.UTF_8));
                } catch (IOException ignored) { /* A program may exit without reading all input. */ }
            });
            try {
                boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
                if (!finished) process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
                int elapsed = (int) TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                String out = stdout.get(2, TimeUnit.SECONDS);
                String err = stderr.get(2, TimeUnit.SECONDS);
                int exit = !finished ? 124 : overflow.get() ? 153 : process.exitValue();
                if (!finished) err = "Time limit exceeded";
                if (overflow.get()) err = "Output limit exceeded (1 MiB per stream)";
                return new ExecutionResult(out, err, exit, elapsed);
            } catch (ExecutionException | TimeoutException e) {
                throw new IOException("Cannot collect sandbox output", e);
            } finally {
                process.destroyForcibly();
                try { process.getOutputStream().close(); } catch (IOException ignored) {}
                writer.cancel(true);
                stdout.cancel(true);
                stderr.cancel(true);
                try { process.getInputStream().close(); } catch (IOException ignored) {}
                try { process.getErrorStream().close(); } catch (IOException ignored) {}
            }
        }
    }

    private String capture(InputStream stream, AtomicBoolean overflow, Process process) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = stream.read(buffer)) != -1) {
            int keep = Math.min(n, OUTPUT_LIMIT - output.size());
            if (keep > 0) output.write(buffer, 0, keep);
            if (keep < n) {
                overflow.set(true);
                process.destroyForcibly();
                break;
            }
        }
        return output.toString(StandardCharsets.UTF_8);
    }

    private void removeContainer(String name) {
        Long end = deadline.get(); deadline.remove();
        try { checked(List.of("docker", "rm", "--force", name)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        catch (IOException e) { log.debug("Container cleanup {}: {}", name, e.getMessage()); }
        finally { if (end != null) deadline.set(end); }
    }

    public void cleanup(Path classDir) {
        if (classDir == null) return;
        Long end = deadline.get(); deadline.remove();
        try { checked(List.of("docker", "volume", "rm", "--force", classDir.getFileName().toString())); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        catch (IOException e) { log.warn("Volume cleanup failed: {}", e.getMessage()); }
        finally { if (end != null) deadline.set(end); }
        try (var paths = Files.walk(classDir)) {
            for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
        } catch (IOException e) { log.warn("Workspace cleanup failed: {}", e.getMessage()); }
    }

    public ExecutionResult runSingle(String sourceCode, String input, int timeLimitMs, int memoryLimitMb)
            throws IOException, InterruptedException {
        CompileResult compile = compile(sourceCode);
        if (!compile.ok()) return new ExecutionResult("", compile.errorMessage(), 2, 0);
        try { return runTest(compile.classDir(), input, timeLimitMs, memoryLimitMb); }
        finally { cleanup(compile.classDir()); }
    }
}
