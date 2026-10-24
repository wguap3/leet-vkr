package com.stackr.runner.service;

import com.stackr.runner.dto.RunRequest;
import com.stackr.runner.dto.RunResponse;
import com.stackr.runner.dto.TestCaseInput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RunnerService — unit tests")
class RunnerServiceTest {

    @Mock DockerService dockerService;
    @InjectMocks RunnerService runnerService;

    private static final Path FAKE_DIR = Path.of("/tmp/fake-class-dir");

    private DockerService.CompileResult okCompile() {
        return new DockerService.CompileResult(true, null, FAKE_DIR);
    }

    private DockerService.CompileResult failCompile(String msg) {
        return new DockerService.CompileResult(false, msg, null);
    }

    private DockerService.ExecutionResult execOk(String stdout, int ms) {
        return new DockerService.ExecutionResult(stdout, "", 0, ms);
    }

    private DockerService.ExecutionResult execRe(int ms) {
        return new DockerService.ExecutionResult("", "Exception in thread...", 1, ms);
    }

    private DockerService.ExecutionResult execTle(int ms) {
        return new DockerService.ExecutionResult("", "Time limit exceeded", 124, ms);
    }

    private RunRequest req(List<TestCaseInput> tcs) {
        return new RunRequest(1L, "public class Solution {}", tcs, 2000, 256);
    }

    // ── CE: compilation fails ─────────────────────────────────────────────────

    @Test
    @DisplayName("run: compile error → CE с сообщением")
    void run_compileError_returnsCe() throws Exception {
        given(dockerService.compile(any())).willReturn(failCompile("';' expected"));

        RunResponse r = runnerService.run(req(List.of(new TestCaseInput("1", "1"))));

        assertThat(r.verdict()).isEqualTo("CE");
        assertThat(r.errorMessage()).contains("';' expected");
        then(dockerService).should(never()).runTest(any(), any(), anyInt(), anyInt());
    }

    // ── compile вызывается РОВНО ОДИН РАЗ при нескольких тест-кейсах ─────────

    @Test
    @DisplayName("run: три теста → compile вызван 1 раз, runTest — 3 раза")
    void run_threeTestCases_compilesOnce() throws Exception {
        given(dockerService.compile(any())).willReturn(okCompile());
        given(dockerService.runTest(eq(FAKE_DIR), any(), anyInt(), anyInt()))
                .willReturn(execOk("1", 100), execOk("2", 110), execOk("3", 90));

        var tcs = List.of(
                new TestCaseInput("in1", "1"),
                new TestCaseInput("in2", "2"),
                new TestCaseInput("in3", "3")
        );
        RunResponse r = runnerService.run(req(tcs));

        assertThat(r.verdict()).isEqualTo("AC");
        then(dockerService).should(times(1)).compile(any());
        then(dockerService).should(times(3)).runTest(eq(FAKE_DIR), any(), anyInt(), anyInt());
        // cleanup called exactly once
        then(dockerService).should(times(1)).cleanup(FAKE_DIR);
    }

    // ── AC: max time правильно агрегируется ───────────────────────────────────

    @Test
    @DisplayName("run: AC → executionTimeMs = max по всем тестам")
    void run_ac_returnsMaxExecutionTime() throws Exception {
        given(dockerService.compile(any())).willReturn(okCompile());
        given(dockerService.runTest(eq(FAKE_DIR), any(), anyInt(), anyInt()))
                .willReturn(execOk("1", 50), execOk("2", 300), execOk("3", 150));

        var tcs = List.of(
                new TestCaseInput("in1", "1"),
                new TestCaseInput("in2", "2"),
                new TestCaseInput("in3", "3")
        );
        RunResponse r = runnerService.run(req(tcs));

        assertThat(r.verdict()).isEqualTo("AC");
        assertThat(r.executionTimeMs()).isEqualTo(300);
    }

    // ── WA: ранний выход на первом неверном ответе ────────────────────────────

    @Test
    @DisplayName("run: WA на втором тесте → третий тест не запускается")
    void run_wrongAnswerOnSecond_stopsEarly() throws Exception {
        given(dockerService.compile(any())).willReturn(okCompile());
        given(dockerService.runTest(eq(FAKE_DIR), any(), anyInt(), anyInt()))
                .willReturn(execOk("1", 100), execOk("WRONG", 100));

        var tcs = List.of(
                new TestCaseInput("in1", "1"),
                new TestCaseInput("in2", "2"),   // WA
                new TestCaseInput("in3", "3")    // should not be reached
        );
        RunResponse r = runnerService.run(req(tcs));

        assertThat(r.verdict()).isEqualTo("WA");
        // runTest called only twice (stops after WA)
        then(dockerService).should(times(2)).runTest(any(), any(), anyInt(), anyInt());
        then(dockerService).should(times(1)).cleanup(FAKE_DIR);
    }

    // ── TLE ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("run: TLE вердикт → возвращает TLE")
    void run_tle_returnsTle() throws Exception {
        given(dockerService.compile(any())).willReturn(okCompile());
        given(dockerService.runTest(eq(FAKE_DIR), any(), anyInt(), anyInt()))
                .willReturn(execTle(2500));

        RunResponse r = runnerService.run(req(List.of(new TestCaseInput("in", "out"))));

        assertThat(r.verdict()).isEqualTo("TLE");
        assertThat(r.executionTimeMs()).isEqualTo(2500);
    }

    // ── RE ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("run: runtime error → RE")
    void run_runtimeError_returnsRe() throws Exception {
        given(dockerService.compile(any())).willReturn(okCompile());
        given(dockerService.runTest(eq(FAKE_DIR), any(), anyInt(), anyInt()))
                .willReturn(execRe(200));

        RunResponse r = runnerService.run(req(List.of(new TestCaseInput("in", "out"))));

        assertThat(r.verdict()).isEqualTo("RE");
    }

    // ── нет тест-кейсов → CE ─────────────────────────────────────────────────

    @Test
    @DisplayName("run: пустой список тест-кейсов → CE")
    void run_noTestCases_returnsCe() throws Exception {
        RunResponse r = runnerService.run(req(List.of()));

        assertThat(r.verdict()).isEqualTo("CE");
        then(dockerService).should(never()).compile(any());
    }

    // ── cleanup вызывается даже при исключении runTest ────────────────────────

    @Test
    @DisplayName("run: runTest выбрасывает исключение → RE, cleanup всё равно вызван")
    void run_runTestThrows_cleanupStillCalled() throws Exception {
        given(dockerService.compile(any())).willReturn(okCompile());
        given(dockerService.runTest(any(), any(), anyInt(), anyInt()))
                .willThrow(new RuntimeException("disk full"));

        RunResponse r = runnerService.run(req(List.of(new TestCaseInput("in", "out"))));

        assertThat(r.verdict()).isEqualTo("RE");
        then(dockerService).should(times(1)).cleanup(FAKE_DIR);
    }
}
