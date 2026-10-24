package com.stackr.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Старый пароль обязателен")
        String oldPassword,

        @NotBlank(message = "Новый пароль обязателен")
        @Size(min = 6, message = "Пароль должен быть не менее 6 символов")
        String newPassword
) {}
