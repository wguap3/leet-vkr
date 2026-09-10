package com.stackr.api.service;

import com.stackr.api.entity.TestCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RunnerClient {

    private final RestTemplate restTemplate;

    @Value("${runner.url}")
    private String runnerUrl;

    public RunnerResponse run(Long submissionId, String sourceCode,
                              List<TestCase> testCases, int timeLimitMs, int memoryLimitMb) {
        var request = Map.of(
                "submissionId", submissionId,
                "sourceCode", sourceCode,
                "testCases", testCases.stream().map(tc -> Map.of(
                        "input", tc.getInputData(),
                        "expectedOutput", tc.getExpectedOutput()
                )).toList(),
                "timeLimitMs", timeLimitMs,
                "memoryLimitMb", memoryLimitMb
        );

        try {
            ResponseEntity<RunnerResponse> response = restTemplate.postForEntity(
                    runnerUrl + "/api/run", request, RunnerResponse.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Runner call failed for submission {}: {}", submissionId, e.getMessage());
            RunnerResponse error = new RunnerResponse();
            error.setSubmissionId(submissionId);
            error.setVerdict("RE");
            error.setErrorMessage("Runner unavailable: " + e.getMessage());
            return error;
        }
    }

    public static class RunnerResponse {
        private Long submissionId;
        private String verdict;
        private Integer executionTimeMs;
        private Integer memoryUsedMb;
        private String errorMessage;

        public Long getSubmissionId() { return submissionId; }
        public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }
        public String getVerdict() { return verdict; }
        public void setVerdict(String verdict) { this.verdict = verdict; }
        public Integer getExecutionTimeMs() { return executionTimeMs; }
        public void setExecutionTimeMs(Integer executionTimeMs) { this.executionTimeMs = executionTimeMs; }
        public Integer getMemoryUsedMb() { return memoryUsedMb; }
        public void setMemoryUsedMb(Integer memoryUsedMb) { this.memoryUsedMb = memoryUsedMb; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}
