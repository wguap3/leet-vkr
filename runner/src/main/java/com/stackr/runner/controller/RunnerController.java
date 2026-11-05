package com.stackr.runner.controller;

import com.stackr.runner.dto.CustomRunRequest;
import com.stackr.runner.dto.CustomRunResponse;
import com.stackr.runner.dto.RunRequest;
import com.stackr.runner.dto.RunResponse;
import com.stackr.runner.service.DockerService;
import com.stackr.runner.service.RunnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RunnerController {

    private final java.util.concurrent.Semaphore jobs = new java.util.concurrent.Semaphore(2);
    private final RunnerService runnerService;
    private final DockerService dockerService;

    @PostMapping("/run")
    public ResponseEntity<RunResponse> run(@jakarta.validation.Valid @RequestBody RunRequest req) {
        if (!jobs.tryAcquire()) return ResponseEntity.status(429).build();
        try { return ResponseEntity.ok(runnerService.run(req)); }
        finally { jobs.release(); }
    }

    /** Custom run: compile + execute with arbitrary input, return raw stdout/stderr. */
    @PostMapping("/run-custom")
    public ResponseEntity<CustomRunResponse> runCustom(@jakarta.validation.Valid @RequestBody CustomRunRequest req) {
        if (!jobs.tryAcquire()) return ResponseEntity.status(429).build();
        try {
            int timeLimit = req.timeLimitMs() > 0 ? req.timeLimitMs() : 5000;
            int memLimit  = req.memoryLimitMb() > 0 ? req.memoryLimitMb() : 256;
            var result = dockerService.runSingle(req.sourceCode(), req.input(), timeLimit, memLimit);

            String status;
            if (result.exitCode() == 2)                                status = "CE";
            else if (result.exitCode() == 124 || result.elapsedMs() > timeLimit) status = "TLE";
            else if (result.exitCode() != 0)                           status = "RE";
            else                                                        status = "OK";

            return ResponseEntity.ok(new CustomRunResponse(
                    result.stdout(), result.stderr(), result.exitCode(), result.elapsedMs(), status));
        } catch (Exception e) {
            return ResponseEntity.ok(new CustomRunResponse("", "Execution service unavailable", -1, 0, "ERROR"));
        } finally { jobs.release(); }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return dockerService.isReady() ? ResponseEntity.ok("OK") : ResponseEntity.status(503).body("Sandbox unavailable");
    }
}
