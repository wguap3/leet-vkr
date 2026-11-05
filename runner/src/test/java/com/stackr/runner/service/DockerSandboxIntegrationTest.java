package com.stackr.runner.service;

import com.stackr.runner.dto.RunRequest;
import com.stackr.runner.dto.TestCaseInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

/** Opt in with STACKR_DOCKER_TESTS=true; requires a running daemon and the sandbox image. */
@EnabledIfEnvironmentVariable(named = "STACKR_DOCKER_TESTS", matches = "true")
class DockerSandboxIntegrationTest {
    private final DockerService docker = new DockerService();
    private final RunnerService runner = new RunnerService(docker);

    private String verdict(String code, String expected) {
        return runner.run(new RunRequest(1L, code, List.of(new TestCaseInput("", expected)), 3000, 256)).verdict();
    }

    @Test void correctSolutionIsAccepted() {
        assertThat(verdict("public class Solution { public static void main(String[] a) { System.out.println(42); } }", "42")).isEqualTo("AC");
    }
    @Test void wrongAnswerIsRejected() {
        assertThat(verdict("public class Solution { public static void main(String[] a) { System.out.println(1); } }", "42")).isEqualTo("WA");
    }
    @Test void compilationFailureIsReported() {
        assertThat(verdict("public class Solution { syntax error }", "42")).isEqualTo("CE");
    }
    @Test void runtimeFailureIsReported() {
        assertThat(verdict("public class Solution { public static void main(String[] a) { throw new RuntimeException(); } }", "42")).isEqualTo("RE");
    }
    @Test void infiniteLoopIsStopped() {
        assertThat(verdict("public class Solution { public static void main(String[] a) { while(true) {} } }", "42")).isEqualTo("TLE");
    }
    @Test void programCannotModifyCompiledClasses() {
        assertThat(verdict("public class Solution { public static void main(String[] a) throws Exception { java.nio.file.Files.writeString(java.nio.file.Path.of(\"/workspace/classes/tampered\"), \"bad\"); } }", "")).isEqualTo("RE");
    }
    @Test void sandboxCannotSeeDockerSocket() {
        assertThat(verdict("public class Solution { public static void main(String[] a) { System.out.println(java.nio.file.Files.exists(java.nio.file.Path.of(\"/var/run/docker.sock\"))); } }", "false")).isEqualTo("AC");
    }
    @Test void excessiveOutputIsStopped() throws Exception {
        var result = docker.runSingle("public class Solution { public static void main(String[] a) { while(true) System.out.println(\"x\".repeat(8192)); } }", "", 10000, 256);
        assertThat(result.exitCode()).isEqualTo(153);
        assertThat(result.stdout().length()).isLessThanOrEqualTo(1024 * 1024);
    }
    @Test void networkAccessIsUnavailable() {
        assertThat(verdict("public class Solution { public static void main(String[] a) { try { new java.net.Socket(\"1.1.1.1\", 80); System.out.println(\"connected\"); } catch (java.io.IOException e) { System.out.println(\"blocked\"); } } }", "blocked")).isEqualTo("AC");
    }
    @Test void heapExhaustionIsContained() {
        assertThat(verdict("public class Solution { public static void main(String[] a) { java.util.List<byte[]> data = new java.util.ArrayList<>(); while(true) data.add(new byte[1000000]); } }", "")).isEqualTo("RE");
    }
    @Test void expiredTotalBudgetDoesNotRunCompilation() {
        docker.beginBudget(-1);
        try {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> docker.compile("class Solution {}"))
                    .isInstanceOf(java.io.IOException.class).hasMessageContaining("budget");
        } finally { docker.endBudget(); }
    }

}
