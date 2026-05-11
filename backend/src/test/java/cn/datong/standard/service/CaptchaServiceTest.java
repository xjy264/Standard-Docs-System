package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.config.CaptchaProperties;
import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.spring.plugins.secondary.SecondaryVerificationApplication;
import cloud.tianai.captcha.validator.impl.SimpleImageCaptchaValidator;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class CaptchaServiceTest {

    @Test
    void verifiedSliderTokenCanPassOnce() {
        SecondaryVerificationApplication captchaApplication = mock(SecondaryVerificationApplication.class);
        when(captchaApplication.secondaryVerification("captcha-id")).thenReturn(true);
        CaptchaService service = new CaptchaService(captchaApplication);

        assertThatCode(() -> service.verify("captcha-id", CaptchaService.SLIDER_PASSED_CODE)).doesNotThrowAnyException();
    }

    @Test
    void sliderVerifyRejectsWrongCode() {
        SecondaryVerificationApplication captchaApplication = mock(SecondaryVerificationApplication.class);
        CaptchaService service = new CaptchaService(captchaApplication);

        assertThatThrownBy(() -> service.verify("captcha-id", "1234"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码错误或已过期");
    }

    @Test
    void sliderVerifyRejectsUnusedToken() {
        SecondaryVerificationApplication captchaApplication = mock(SecondaryVerificationApplication.class);
        when(captchaApplication.secondaryVerification("captcha-id")).thenReturn(false);
        CaptchaService service = new CaptchaService(captchaApplication);

        assertThatThrownBy(() -> service.verify("captcha-id", CaptchaService.SLIDER_PASSED_CODE))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码错误或已过期");
    }

    @Test
    void disabledProviderSkipsSliderVerification() {
        ImageCaptchaApplication captchaApplication = mock(ImageCaptchaApplication.class);
        CaptchaProperties properties = new CaptchaProperties();
        properties.setProvider("none");
        CaptchaService service = new CaptchaService(captchaApplication, properties);

        assertThatCode(() -> service.verify("", "")).doesNotThrowAnyException();
    }

    @Test
    void matchingConvertsAbsoluteBrowserTrackToMovePercentage() {
        ImageCaptchaApplication captchaApplication = mock(ImageCaptchaApplication.class);
        when(captchaApplication.matching(eq("captcha-id"), anyFloat())).thenReturn(true);
        CaptchaService service = new CaptchaService(captchaApplication);
        ImageCaptchaTrack track = new ImageCaptchaTrack();
        track.setBgImageWidth(300);
        track.setTrackList(List.of(
                new ImageCaptchaTrack.Track(520f, 260f, 0f, "down"),
                new ImageCaptchaTrack.Track(620f, 261f, 300f, "move"),
                new ImageCaptchaTrack.Track(642f, 260f, 500f, "up")
        ));

        service.matching("captcha-id", track);

        ArgumentCaptor<Float> captor = ArgumentCaptor.forClass(Float.class);
        verify(captchaApplication).matching(eq("captcha-id"), captor.capture());
        verify(captchaApplication, never()).matching(eq("captcha-id"), org.mockito.ArgumentMatchers.any(ImageCaptchaTrack.class));
        assertThat(captor.getValue()).isCloseTo(122f / 300f, org.assertj.core.data.Offset.offset(0.0001f));
    }

    @Test
    void matchingFailureUsesTacBasicCheckFailCode() {
        ImageCaptchaApplication captchaApplication = mock(ImageCaptchaApplication.class);
        when(captchaApplication.matching(eq("captcha-id"), anyFloat())).thenReturn(false);
        CaptchaService service = new CaptchaService(captchaApplication);
        ImageCaptchaTrack track = new ImageCaptchaTrack();
        track.setBgImageWidth(300);
        track.setTrackList(List.of(
                new ImageCaptchaTrack.Track(0f, 0f, 0f, "down"),
                new ImageCaptchaTrack.Track(100f, 0f, 500f, "up")
        ));

        cloud.tianai.captcha.common.response.ApiResponse<?> response = service.matching("captcha-id", track);

        assertThat(response.getCode()).isEqualTo(4001);
    }

    @Test
    void matchingWidensSliderToleranceForLocalUiScale() {
        ImageCaptchaApplication captchaApplication = mock(ImageCaptchaApplication.class);
        SimpleImageCaptchaValidator validator = new SimpleImageCaptchaValidator(0.02f);
        when(captchaApplication.getImageCaptchaValidator()).thenReturn(validator);
        when(captchaApplication.matching(eq("captcha-id"), anyFloat())).thenReturn(true);
        CaptchaService service = new CaptchaService(captchaApplication);
        ImageCaptchaTrack track = new ImageCaptchaTrack();
        track.setBgImageWidth(300);
        track.setTrackList(List.of(
                new ImageCaptchaTrack.Track(0f, 0f, 0f, "down"),
                new ImageCaptchaTrack.Track(100f, 0f, 500f, "up")
        ));

        service.matching("captcha-id", track);

        assertThat(validator.getDefaultTolerant()).isEqualTo(0.08f);
    }

    @Test
    void matchedSliderTokenCanPassServiceVerificationOnce() {
        ImageCaptchaApplication captchaApplication = mock(ImageCaptchaApplication.class);
        when(captchaApplication.matching(eq("captcha-id"), anyFloat())).thenReturn(true);
        CaptchaService service = new CaptchaService(captchaApplication);
        ImageCaptchaTrack track = new ImageCaptchaTrack();
        track.setBgImageWidth(300);
        track.setTrackList(List.of(
                new ImageCaptchaTrack.Track(520f, 260f, 0f, "down"),
                new ImageCaptchaTrack.Track(560f, 260f, 160f, "move"),
                new ImageCaptchaTrack.Track(610f, 260f, 320f, "move"),
                new ImageCaptchaTrack.Track(642f, 260f, 500f, "up")
        ));

        service.matching("captcha-id", track);

        assertThatCode(() -> service.verify("captcha-id", CaptchaService.SLIDER_PASSED_CODE)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.verify("captcha-id", CaptchaService.SLIDER_PASSED_CODE))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码错误或已过期");
    }
}
