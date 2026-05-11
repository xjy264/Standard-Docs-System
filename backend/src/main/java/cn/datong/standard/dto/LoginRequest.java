package cn.datong.standard.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password,
        String captchaKey,
        String captchaCode
) {
}
