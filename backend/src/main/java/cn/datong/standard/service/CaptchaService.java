package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.config.CaptchaProperties;
import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.common.response.ApiResponse;
import cloud.tianai.captcha.common.response.ApiResponseStatusConstant;
import cloud.tianai.captcha.spring.plugins.secondary.SecondaryVerificationApplication;
import cloud.tianai.captcha.validator.impl.SimpleImageCaptchaValidator;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CaptchaService {
    public static final String SLIDER_PASSED_CODE = "SLIDER_PASSED";
    private static final long PASSED_CAPTCHA_EXPIRE_MILLIS = 5 * 60 * 1000L;
    private static final float SLIDER_TOLERANCE = 0.08f;
    private final ImageCaptchaApplication captchaApplication;
    private final CaptchaProperties captchaProperties;
    private final Map<String, Long> passedCaptchaIds = new ConcurrentHashMap<>();

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
        Float movePercentage = calculateMovePercentage(track);
        applySliderTolerance();
        if (movePercentage == null || !captchaApplication.matching(id, movePercentage)) {
            return ApiResponse.ofMessage(ApiResponseStatusConstant.BASIC_CHECK_FAIL);
        }
        passedCaptchaIds.put(id, System.currentTimeMillis() + PASSED_CAPTCHA_EXPIRE_MILLIS);
        return ApiResponse.ofSuccess();
    }

    private void applySliderTolerance() {
        if (captchaApplication.getImageCaptchaValidator() instanceof SimpleImageCaptchaValidator validator
                && validator.getDefaultTolerant() < SLIDER_TOLERANCE) {
            validator.setDefaultTolerant(SLIDER_TOLERANCE);
        }
    }

    private Float calculateMovePercentage(ImageCaptchaTrack track) {
        if (track == null || track.getBgImageWidth() == null || track.getBgImageWidth() <= 0
                || track.getTrackList() == null || track.getTrackList().size() < 2) {
            return null;
        }
        ImageCaptchaTrack.Track firstTrack = track.getTrackList().get(0);
        ImageCaptchaTrack.Track lastTrack = track.getTrackList().get(track.getTrackList().size() - 1);
        if (firstTrack.getX() == null || lastTrack.getX() == null) {
            return null;
        }
        return (lastTrack.getX() - firstTrack.getX()) / track.getBgImageWidth();
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
        if (!SLIDER_PASSED_CODE.equals(code)) {
            throw new BusinessException("验证码错误或已过期");
        }
        Long expireAt = passedCaptchaIds.remove(key);
        if (expireAt != null && expireAt >= System.currentTimeMillis()) {
            return;
        }
        if (!(captchaApplication instanceof SecondaryVerificationApplication secondary)
                || !secondary.secondaryVerification(key)) {
            throw new BusinessException("验证码错误或已过期");
        }
    }
}
