package com.stackr.runner.service;

import com.stackr.runner.dto.RunRequest;
import com.stackr.runner.dto.RunResponse;
import com.stackr.runner.dto.TestCaseInput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

        // First, check compilation by doing a dry run with the first test case
        try {
            for (TestCaseInput tc : req.testCases()) {
                var result = dockerService.runInDocker(
                        req.sourceCode(), tc.input(), req.timeLimitMs(), req.memoryLimitMb());

                // Exit code 2 = compilation error
                if (result.exitCode() == 2) {
                    return RunResponse.builder()
                            .submissionId(req.submissionId())
                            .verdict("CE")
                            .errorMessage(truncate(result.stderr(), 2000))
                            .build();
                }

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

                // Wrong answer
                String expected = tc.expectedOutput().trim();
                String actual = result.stdout().trim();
                if (!expected.equals(actual)) {
                    return RunResponse.builder()
                            .submissionId(req.submissionId())
                            .verdict("WA")
                            .executionTimeMs(result.elapsedMs())
                            .build();
                }
            }

            // All test cases passed
            var lastResult = dockerService.runInDocker(
                    req.sourceCode(),
                    req.testCases().get(req.testCases().size() - 1).input(),
                    req.timeLimitMs(), req.memoryLimitMb());

            return RunResponse.builder()
                    .submissionId(req.submissionId())
                    .verdict("AC")
                    .executionTimeMs(lastResult.elapsedMs())
                    .memoryUsedMb(req.memoryLimitMb() / 4) // estimate
                    .build();

        } catch (Exception e) {
            log.error("Runner error for submission {}", req.submissionId(), e);
            return RunResponse.builder()
                    .submissionId(req.submissionId())
                    .verdict("RE")
                    .errorMessage("Internal runner error: " + e.getMessage())
                    .build();
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
