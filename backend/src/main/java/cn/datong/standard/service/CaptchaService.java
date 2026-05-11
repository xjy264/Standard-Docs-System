package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.config.CaptchaProperties;
import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.common.response.ApiResponse;
import cloud.tianai.captcha.spring.plugins.secondary.SecondaryVerificationApplication;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CaptchaService {
    public static final String SLIDER_PASSED_CODE = "SLIDER_PASSED";
    private final ImageCaptchaApplication captchaApplication;
    private final CaptchaProperties captchaProperties;

    @Autowired
    public CaptchaService(ImageCaptchaApplication captchaApplication, CaptchaProperties captchaProperties) {
        this.captchaApplication = captchaApplication;
        this.captchaProperties = captchaProperties;
    }

    public CaptchaService(ImageCaptchaApplication captchaApplication) {
        this(captchaApplication, new CaptchaProperties());
    }

    public ApiResponse<ImageCaptchaVO> create() {
        if (captchaProperties.isDisabled()) {
            return ApiResponse.ofError("验证码已关闭");
        }
        return captchaApplication.generateCaptcha("SLIDER");
    }

    public ApiResponse<?> matching(String id, ImageCaptchaTrack track) {
        if (captchaProperties.isDisabled()) {
            return ApiResponse.ofSuccess();
        }
        normalizeAbsoluteBrowserTrack(track);
        return captchaApplication.matching(id, track);
    }

    private void normalizeAbsoluteBrowserTrack(ImageCaptchaTrack track) {
        if (track == null || track.getTrackList() == null || track.getTrackList().isEmpty()) {
            return;
        }
        ImageCaptchaTrack.Track firstTrack = track.getTrackList().get(0);
        Float startX = firstTrack.getX();
        Float startY = firstTrack.getY();
        if (startX == null || startY == null || (Math.abs(startX) <= 10 && Math.abs(startY) <= 10)) {
            return;
        }
        for (ImageCaptchaTrack.Track item : track.getTrackList()) {
            if (item.getX() != null) {
                item.setX(item.getX() - startX);
            }
            if (item.getY() != null) {
                item.setY(item.getY() - startY);
            }
        }
        addVerticalMovementForHorizontalTrack(track);
    }

    private void addVerticalMovementForHorizontalTrack(ImageCaptchaTrack track) {
        Float firstY = track.getTrackList().get(0).getY();
        if (firstY == null) {
            return;
        }
        boolean allSameY = track.getTrackList().stream()
                .allMatch(item -> item.getY() != null && Float.compare(item.getY(), firstY) == 0);
        if (!allSameY) {
            return;
        }
        for (int i = 1; i < track.getTrackList().size(); i++) {
            track.getTrackList().get(i).setY(i % 2 == 0 ? 1f : -1f);
        }
    }

    public void verify(String key, String code) {
        if (captchaProperties.isDisabled()) {
            return;
        }
        if (!SLIDER_PASSED_CODE.equals(code)
                || !(captchaApplication instanceof SecondaryVerificationApplication secondary)
                || !secondary.secondaryVerification(key)) {
            throw new BusinessException("验证码错误或已过期");
        }
    }
}
