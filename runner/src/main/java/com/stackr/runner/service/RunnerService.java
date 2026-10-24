package com.stackr.runner.service;

import com.stackr.runner.dto.RunRequest;
import com.stackr.runner.dto.RunResponse;
import com.stackr.runner.dto.TestCaseInput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Judges a submission against all test cases.
 *
 * Optimised flow:
 *   1. Compile the source code ONCE.
 *   2. Iterate test cases, running the compiled binary for each.
 *   3. Return on the first non-AC verdict (fail-fast).
 *   4. Clean up the temp directory.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RunnerService {

    private final DockerService dockerService;

    public RunResponse run(RunRequest req) {
        if (req.testCases() == null || req.testCases().isEmpty()) {
            return RunResponse.builder()
                    .submissionId(req.submissionId())
                    .verdict("CE")
                    .errorMessage("No test cases provided")
                    .build();
        }

        // ── 1. Compile once ───────────────────────────────────────────────────
        DockerService.CompileResult compile;
        try {
            compile = dockerService.compile(req.sourceCode());
        } catch (Exception e) {
            log.error("Compile exception for submission {}: {}", req.submissionId(), e.getMessage());
            return RunResponse.builder()
                    .submissionId(req.submissionId())
                    .verdict("CE")
                    .errorMessage("Internal compile error: " + e.getMessage())
                    .build();
        }

        if (!compile.ok()) {
            return RunResponse.builder()
                    .submissionId(req.submissionId())
                    .verdict("CE")
                    .errorMessage(truncate(compile.errorMessage(), 2000))
                    .build();
        }

        // ── 2. Run each test case (fail-fast) ─────────────────────────────────
        int maxElapsedMs = 0;
        try {
            for (TestCaseInput tc : req.testCases()) {
                DockerService.ExecutionResult result;
                try {
                    result = dockerService.runTest(
                            compile.classDir(), tc.input(), req.timeLimitMs(), req.memoryLimitMb());
                } catch (Exception e) {
                    log.error("runTest exception for submission {}: {}", req.submissionId(), e.getMessage());
                    return RunResponse.builder()
                            .submissionId(req.submissionId())
                            .verdict("RE")
                            .errorMessage("Internal runner error: " + e.getMessage())
                            .build();
                }

                maxElapsedMs = Math.max(maxElapsedMs, result.elapsedMs());

                // TLE
                if (result.exitCode() == 124 || result.elapsedMs() > req.timeLimitMs()) {
                    return RunResponse.builder()
                            .submissionId(req.submissionId())
                            .verdict("TLE")
                            .executionTimeMs(result.elapsedMs())
                            .build();
                }

                // Runtime error
                if (result.exitCode() != 0) {
                    return RunResponse.builder()
                            .submissionId(req.submissionId())
                            .verdict("RE")
                            .executionTimeMs(result.elapsedMs())
                            .errorMessage(truncate(result.stderr(), 2000))
                            .build();
                }

                // Wrong answer (trim both sides for trailing newline tolerance)
                if (!tc.expectedOutput().trim().equals(result.stdout().trim())) {
                    return RunResponse.builder()
                            .submissionId(req.submissionId())
                            .verdict("WA")
                            .executionTimeMs(result.elapsedMs())
                            .build();
                }
            }

            // ── 3. All tests passed ───────────────────────────────────────────
            return RunResponse.builder()
                    .submissionId(req.submissionId())
                    .verdict("AC")
                    .executionTimeMs(maxElapsedMs)
                    .memoryUsedMb(req.memoryLimitMb() / 4)   // rough estimate
                    .build();

        } finally {
            // ── 4. Always clean up the compiled classes ───────────────────────
            dockerService.cleanup(compile.classDir());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
