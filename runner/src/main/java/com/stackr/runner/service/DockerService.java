package com.stackr.runner.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

/**
 * Executes Java source code directly in the runner container (JDK is available).
 *
 * Two-phase execution:
 *   1. compile(sourceCode) — compiles once into a temporary directory
 *   2. runTest(classDir, input, timeLimitMs, memoryLimitMb) — run one test case
 *
 * Call compile() once per submission, then runTest() for each test case.
 * Use cleanup(classDir) to delete the temp directory when done.
 *
 * runSingle() is kept for custom-run requests (compile + run in one call).
 */
@Slf4j
@Service
public class DockerService {

    // ── Records ───────────────────────────────────────────────────────────────

    public record CompileResult(boolean ok, String errorMessage, Path classDir) {}
    public record ExecutionResult(String stdout, String stderr, int exitCode, int elapsedMs) {}

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Compile {@code sourceCode} and return the path to the directory holding
     * the .class files (or an error result if compilation failed).
     * The caller MUST call {@link #cleanup(Path)} when done with all test runs.
     */
    public CompileResult compile(String sourceCode) throws IOException, InterruptedException {
        Path workDir = Files.createTempDirectory("stackr-compile-");
        Path srcFile = workDir.resolve("Solution.java");
        Files.writeString(srcFile, sourceCode, StandardCharsets.UTF_8);

        Process proc = new ProcessBuilder("javac", srcFile.toString())
                .directory(workDir.toFile())
                .redirectErrorStream(true)
                .start();

        boolean finished = proc.waitFor(30, TimeUnit.SECONDS);
        String output = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        if (!finished) {
            proc.destroyForcibly();
            cleanup(workDir);
            return new CompileResult(false, "Compilation timed out", null);
        }
        if (proc.exitValue() != 0) {
            cleanup(workDir);
            log.debug("Compile error: {}", output);
            return new CompileResult(false, output.trim(), null);
        }

        return new CompileResult(true, null, workDir);
    }

    /**
     * Run {@code Solution} class (already compiled into {@code classDir}) with
     * the given stdin {@code input}. Does NOT recompile.
     */
    public ExecutionResult runTest(Path classDir, String input, int timeLimitMs, int memoryLimitMb)
            throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-Xmx" + memoryLimitMb + "m",
                "-Xss8m",
                "-cp", classDir.toString(),
                "Solution"
        ).directory(classDir.toFile());

        long start = System.currentTimeMillis();
        Process proc = pb.start();

        try (OutputStream stdin = proc.getOutputStream()) {
            if (input != null && !input.isBlank()) {
                stdin.write(input.getBytes(StandardCharsets.UTF_8));
            }
        }

        boolean finished = proc.waitFor(timeLimitMs + 5000L, TimeUnit.MILLISECONDS);
        long elapsed = System.currentTimeMillis() - start;

        if (!finished) {
            proc.destroyForcibly();
            return new ExecutionResult("", "Time limit exceeded", 124, (int) elapsed);
        }

        String stdout = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(proc.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = proc.exitValue();
        log.debug("runTest exit={} elapsed={}ms", exitCode, elapsed);
        return new ExecutionResult(stdout.trim(), stderr.trim(), exitCode, (int) elapsed);
    }

    /**
     * Delete the temp directory created by {@link #compile(String)}.
     */
    public void cleanup(Path classDir) {
        if (classDir == null) return;
        try {
            Files.walk(classDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        } catch (IOException ignored) {}
    }

    /**
     * Convenience method: compile + run with a single input (used for custom-run endpoint).
     * Exit code 2 signals a compile error.
     */
    public ExecutionResult runSingle(String sourceCode, String input, int timeLimitMs, int memoryLimitMb)
            throws IOException, InterruptedException {

        CompileResult compile = compile(sourceCode);
        if (!compile.ok()) {
            // exit code 2 = compile error (preserved from original contract)
            return new ExecutionResult("", compile.errorMessage(), 2, 0);
        }
        try {
            return runTest(compile.classDir(), input, timeLimitMs, memoryLimitMb);
        } finally {
            cleanup(compile.classDir());
        }
    }
}
