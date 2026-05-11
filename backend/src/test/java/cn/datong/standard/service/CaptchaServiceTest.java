package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.spring.plugins.secondary.SecondaryVerificationApplication;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
    void noneProviderSkipsSliderVerificationForLocalTests() {
        ImageCaptchaApplication captchaApplication = mock(ImageCaptchaApplication.class);
        CaptchaService service = new CaptchaService(captchaApplication, "none");

        assertThatCode(() -> service.verify("captcha-id", "any-code")).doesNotThrowAnyException();
    }

    @Test
    void matchingNormalizesAbsoluteBrowserTrackToRelativeTrack() {
        ImageCaptchaApplication captchaApplication = mock(ImageCaptchaApplication.class);
        CaptchaService service = new CaptchaService(captchaApplication);
        ImageCaptchaTrack track = new ImageCaptchaTrack();
        track.setTrackList(List.of(
                new ImageCaptchaTrack.Track(520f, 260f, 0f, "down"),
                new ImageCaptchaTrack.Track(620f, 261f, 300f, "move"),
                new ImageCaptchaTrack.Track(642f, 260f, 500f, "up")
        ));

        service.matching("captcha-id", track);

        ArgumentCaptor<ImageCaptchaTrack> captor = ArgumentCaptor.forClass(ImageCaptchaTrack.class);
        verify(captchaApplication).matching(eq("captcha-id"), captor.capture());
        List<ImageCaptchaTrack.Track> normalized = captor.getValue().getTrackList();
        assertThat(normalized.get(0).getX()).isEqualTo(0f);
        assertThat(normalized.get(0).getY()).isEqualTo(0f);
        assertThat(normalized.get(1).getX()).isEqualTo(100f);
        assertThat(normalized.get(1).getY()).isEqualTo(1f);
        assertThat(normalized.get(2).getX()).isEqualTo(122f);
        assertThat(normalized.get(2).getY()).isEqualTo(0f);
    }

    @Test
    void matchingAddsTinyVerticalMovementWhenBrowserTrackIsPerfectlyHorizontal() {
        ImageCaptchaApplication captchaApplication = mock(ImageCaptchaApplication.class);
        CaptchaService service = new CaptchaService(captchaApplication);
        ImageCaptchaTrack track = new ImageCaptchaTrack();
        track.setTrackList(List.of(
                new ImageCaptchaTrack.Track(520f, 260f, 0f, "down"),
                new ImageCaptchaTrack.Track(560f, 260f, 160f, "move"),
                new ImageCaptchaTrack.Track(610f, 260f, 320f, "move"),
                new ImageCaptchaTrack.Track(642f, 260f, 500f, "up")
        ));

        service.matching("captcha-id", track);

        ArgumentCaptor<ImageCaptchaTrack> captor = ArgumentCaptor.forClass(ImageCaptchaTrack.class);
        verify(captchaApplication).matching(eq("captcha-id"), captor.capture());
        List<Float> yPoints = captor.getValue().getTrackList().stream()
                .map(ImageCaptchaTrack.Track::getY)
                .toList();
        assertThat(yPoints).contains(0f);
        assertThat(yPoints.stream().distinct()).hasSizeGreaterThan(1);
        assertThat(yPoints).allSatisfy(y -> assertThat(Math.abs(y)).isLessThanOrEqualTo(2f));
    }
}
