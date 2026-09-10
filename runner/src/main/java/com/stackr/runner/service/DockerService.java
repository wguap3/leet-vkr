package com.stackr.runner.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

/**
 * Executes Java source code directly in the runner container (JDK is available).
 * The runner container itself is already isolated via Docker Compose —
 * no nested Docker-in-Docker required.
 */
@Slf4j
@Service
public class DockerService {

    public ExecutionResult runInDocker(String sourceCode, String input, int timeLimitMs, int memoryLimitMb)
            throws IOException, InterruptedException {

        Path workDir = Files.createTempDirectory("stackr-run-");
        try {
            Path srcFile = workDir.resolve("Solution.java");
            Files.writeString(srcFile, sourceCode, StandardCharsets.UTF_8);

            // ── 1. Compile ────────────────────────────────────────────────
            Process compile = new ProcessBuilder("javac", srcFile.toString())
                    .directory(workDir.toFile())
                    .redirectErrorStream(true)
                    .start();

            boolean compileFinished = compile.waitFor(10, TimeUnit.SECONDS);
            String compileOutput = new String(compile.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            if (!compileFinished) {
                compile.destroyForcibly();
                return new ExecutionResult("", "Compile timeout", 2, 10000);
            }
            if (compile.exitValue() != 0) {
                log.debug("Compile error: {}", compileOutput);
                return new ExecutionResult("", compileOutput.trim(), 2, 0);
            }

            // ── 2. Run ────────────────────────────────────────────────────
            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "-Xmx" + memoryLimitMb + "m",
                    "-Xss8m",
                    "-cp", workDir.toString(),
                    "Solution"
            ).directory(workDir.toFile());

            long start = System.currentTimeMillis();
            Process run = pb.start();

            // Feed input via stdin
            try (OutputStream stdin = run.getOutputStream()) {
                if (input != null && !input.isBlank()) {
                    stdin.write(input.getBytes(StandardCharsets.UTF_8));
                }
            }

            boolean finished = run.waitFor(timeLimitMs + 5000L, TimeUnit.MILLISECONDS);
            long elapsed = System.currentTimeMillis() - start;

            if (!finished) {
                run.destroyForcibly();
                return new ExecutionResult("", "Time limit exceeded", 124, (int) elapsed);
            }

            String stdout = new String(run.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(run.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = run.exitValue();

            log.debug("Run exit={} elapsed={}ms", exitCode, elapsed);
            return new ExecutionResult(stdout.trim(), stderr.trim(), exitCode, (int) elapsed);

        } finally {
            try {
                Files.walk(workDir)
                        .sorted((a, b) -> -a.compareTo(b))
                        .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
            } catch (IOException ignored) {}
        }
    }

    public record ExecutionResult(String stdout, String stderr, int exitCode, int elapsedMs) {}
}
