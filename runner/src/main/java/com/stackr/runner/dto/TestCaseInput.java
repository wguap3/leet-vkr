package com.stackr.runner.dto;
import jakarta.validation.constraints.*;

public record TestCaseInput(@NotNull @Size(max = 1048576) String input, @NotNull @Size(max = 1048576) String expectedOutput) {}
