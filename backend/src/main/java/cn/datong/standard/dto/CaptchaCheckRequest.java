package cn.datong.standard.dto;

import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CaptchaCheckRequest(
        @NotBlank String id,
        @NotNull ImageCaptchaTrack data
) {
}
