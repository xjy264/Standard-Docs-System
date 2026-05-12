package cn.datong.standard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        String password,
        String confirmPassword,
        @NotBlank String realName,
        @NotBlank String phone,
        @NotNull Long deptId,
        String captchaKey,
        String captchaCode
) {
}
