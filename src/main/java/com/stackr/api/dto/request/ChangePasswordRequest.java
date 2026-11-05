package com.stackr.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Старый пароль обязателен")
        String oldPassword,

        @NotBlank(message = "Новый пароль обязателен")
        @Size(min = 8, max = 100, message = "Пароль должен содержать от 8 до 100 символов")
        String newPassword
) {}
