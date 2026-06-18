package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.AuthSessionResponse;
import cn.datong.standard.dto.AuthTokenResponse;
import cn.datong.standard.dto.AuthUser;
import cn.datong.standard.dto.CaptchaCheckRequest;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.dto.LoginRequest;
import cn.datong.standard.dto.RegisterRequest;
import cn.datong.standard.security.AuthCookies;
import cn.datong.standard.security.SecurityUtils;
import cn.datong.standard.service.AuthService;
import cn.datong.standard.service.CaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private static final String CAPTCHA_UNAVAILABLE_MESSAGE = "人机验证暂时不可用，请稍后重试或联系管理员。";
    private static final String CAPTCHA_CHECK_FAILED_MESSAGE = "滑块验证未通过，请重新验证。";
    private final CaptchaService captchaService;
    private final AuthService authService;
    @Value("${app.auth.cookie-secure:false}")
    private boolean cookieSecure;
    @Value("${app.jwt.expire-seconds:86400}")
    private long jwtExpireSeconds;
    private static final SecureRandom RANDOM = new SecureRandom();

    @RequestMapping(value = "/captcha", method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResponse<?> captcha() {
        try {
            cloud.tianai.captcha.common.response.ApiResponse<?> response = captchaService.create();
            if (!response.isSuccess()) {
                return ApiResponse.fail(response.getCode(), CAPTCHA_UNAVAILABLE_MESSAGE);
            }
            return ApiResponse.success(response.getData());
        } catch (Exception ex) {
            log.error("生成滑块验证码失败", ex);
            return ApiResponse.fail(500, CAPTCHA_UNAVAILABLE_MESSAGE);
        }
    }

    @PostMapping("/captcha/check")
    public ApiResponse<Map<String, String>> checkCaptcha(@Valid @RequestBody CaptchaCheckRequest request) {
        cloud.tianai.captcha.common.response.ApiResponse<?> response = captchaService.matching(request.id(), request.data());
        if (!response.isSuccess()) {
            return ApiResponse.fail(response.getCode(), CAPTCHA_CHECK_FAILED_MESSAGE);
        }
        return ApiResponse.success(Map.of(
                "captchaKey", request.id(),
                "captchaCode", CaptchaService.SLIDER_PASSED_CODE
        ));
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success();
    }

    @PostMapping("/login")
    public ApiResponse<AuthSessionResponse> login(@Valid @RequestBody LoginRequest request,
                                                  HttpServletRequest servletRequest,
                                                  HttpServletResponse servletResponse) {
        AuthTokenResponse response = authService.login(request, servletRequest);
        setCookie(servletResponse, AuthCookies.AUTH_COOKIE, response.token(), true, Duration.ofSeconds(jwtExpireSeconds));
        setCookie(servletResponse, AuthCookies.CSRF_COOKIE, randomToken(), false, Duration.ofSeconds(jwtExpireSeconds));
        return ApiResponse.success(AuthSessionResponse.from(response));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse servletResponse) {
        clearCookie(servletResponse, AuthCookies.AUTH_COOKIE, true);
        clearCookie(servletResponse, AuthCookies.CSRF_COOKIE, false);
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<AuthSessionResponse> me() {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(authService.currentSession(currentUser.userId()));
    }

    private void setCookie(HttpServletResponse response, String name, String value, boolean httpOnly, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearCookie(HttpServletResponse response, String name, boolean httpOnly) {
        setCookie(response, name, "", httpOnly, Duration.ZERO);
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
