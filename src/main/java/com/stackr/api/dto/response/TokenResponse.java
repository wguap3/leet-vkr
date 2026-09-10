package com.stackr.api.dto.response;

public record TokenResponse(String accessToken, String refreshToken, long expiresIn) {}
