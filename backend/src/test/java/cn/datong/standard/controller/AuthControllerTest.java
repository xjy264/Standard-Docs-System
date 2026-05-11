package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.mapper.SysUserMapper;
import cn.datong.standard.service.AuthService;
import cn.datong.standard.service.CaptchaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    @Test
    void captchaReturnsStandardFailureWhenGeneratorThrows() {
        CaptchaService captchaService = mock(CaptchaService.class);
        when(captchaService.create()).thenThrow(new IllegalStateException("resource missing"));
        AuthController controller = new AuthController(
                captchaService,
                mock(AuthService.class),
                mock(SysUserMapper.class),
                new ObjectMapper()
        );

        ApiResponse<?> response = controller.captcha();

        assertThat(response.getCode()).isEqualTo(500);
        assertThat(response.getMessage()).isEqualTo("验证码服务异常，请稍后重试");
        assertThat(response.getData()).isNull();
    }

    @Test
    void captchaKeepsTacCompatibleSuccessPayload() {
        CaptchaService captchaService = mock(CaptchaService.class);
        when(captchaService.create()).thenReturn((cloud.tianai.captcha.common.response.ApiResponse) cloud.tianai.captcha.common.response.ApiResponse.ofSuccess(Map.of(
                "type", "SLIDER",
                "backgroundImage", "data:image/jpeg;base64,test",
                "templateImage", "data:image/png;base64,test"
        )));
        AuthController controller = new AuthController(
                captchaService,
                mock(AuthService.class),
                mock(SysUserMapper.class),
                new ObjectMapper()
        );

        ApiResponse<?> response = controller.captcha();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isInstanceOf(Map.class);
        Map<?, ?> data = (Map<?, ?>) response.getData();
        assertThat(data.get("type")).isEqualTo("SLIDER");
    }
}
