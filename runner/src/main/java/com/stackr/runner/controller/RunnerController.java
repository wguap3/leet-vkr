package com.stackr.runner.controller;

import com.stackr.runner.dto.RunRequest;
import com.stackr.runner.dto.RunResponse;
import com.stackr.runner.service.RunnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RunnerController {

    private final RunnerService runnerService;

    @PostMapping("/run")
    public ResponseEntity<RunResponse> run(@RequestBody RunRequest req) {
        return ResponseEntity.ok(runnerService.run(req));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
