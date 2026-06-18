package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.AuthTokenResponse;
import cn.datong.standard.dto.AuthUser;
import cn.datong.standard.service.AuthService;
import cn.datong.standard.service.CaptchaService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    @Test
    void captchaReturnsUserFriendlyFailureWhenGeneratorThrows() {
        CaptchaService captchaService = mock(CaptchaService.class);
        when(captchaService.create()).thenThrow(new IllegalStateException("resource missing"));
        AuthController controller = new AuthController(
                captchaService,
                mock(AuthService.class)
        );

        ApiResponse<?> response = controller.captcha();

        assertThat(response.getCode()).isEqualTo(500);
        assertThat(response.getMessage()).isEqualTo("人机验证暂时不可用，请稍后重试或联系管理员。");
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
                mock(AuthService.class)
        );

        ApiResponse<?> response = controller.captcha();

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isInstanceOf(Map.class);
        Map<?, ?> data = (Map<?, ?>) response.getData();
        assertThat(data.get("type")).isEqualTo("SLIDER");
    }

    @Test
    void controllerDoesNotExposeDevLoginEndpoint() {
        assertThat(Stream.of(AuthController.class.getDeclaredMethods()).map(java.lang.reflect.Method::getName))
                .doesNotContain("devLogin");
    }

    @Test
    void loginSetsHttpOnlyCookieAndDoesNotExposeTokenInBody() throws Exception {
        AuthService authService = mock(AuthService.class);
        AuthUser user = new AuthUser(1L, "admin", "系统管理员", "00000000000", 1L, true, true);
        when(authService.login(any(), any())).thenReturn(new AuthTokenResponse("token", user, Set.of("*")));
        AuthController controller = new AuthController(
                mock(CaptchaService.class),
                authService
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "00000000000",
                                  "password": "Admin12345@@",
                                  "captchaKey": "captcha-id",
                                  "captchaCode": "SLIDER_PASSED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("SDS_AUTH", true))
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(jsonPath("$.data.token").doesNotExist())
                .andExpect(jsonPath("$.data.user.id").value(1))
                .andExpect(jsonPath("$.data.permissions[0]").value("*"));
    }
}
