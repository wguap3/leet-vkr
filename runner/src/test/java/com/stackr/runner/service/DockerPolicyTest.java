package com.stackr.runner.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DockerPolicyTest {
    @Test void executionUsesReadOnlyClassesAndRestrictedContainer() {
        var args = DockerService.containerArgs("job", "volume", true, 256);
        assertThat(args).containsSubsequence("--network", "none");
        assertThat(args).containsSubsequence("--user", "65534:65534");
        assertThat(args).containsSubsequence("--memory", "256m", "--memory-swap", "256m");
        assertThat(args).containsSubsequence("--security-opt", "no-new-privileges:true");
        assertThat(args).contains("--read-only", "type=volume,src=volume,dst=/workspace,readonly");
        assertThat(args).doesNotContain("--privileged", "/var/run/docker.sock");
    }
}
