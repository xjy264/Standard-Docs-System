package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.AuthTokenResponse;
import cn.datong.standard.dto.CaptchaCheckRequest;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.dto.LoginRequest;
import cn.datong.standard.dto.RegisterRequest;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.mapper.SysUserMapper;
import cn.datong.standard.security.SecurityUtils;
import cn.datong.standard.service.AuthService;
import cn.datong.standard.service.CaptchaService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final CaptchaService captchaService;
    private final AuthService authService;
    private final SysUserMapper userMapper;
    private final ObjectMapper objectMapper;

    @RequestMapping(value = "/captcha", method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResponse<?> captcha() {
        cloud.tianai.captcha.common.response.ApiResponse<?> response = captchaService.create();
        if (!response.isSuccess()) {
            return ApiResponse.fail(response.getCode(), response.getMsg());
        }
        return ApiResponse.success(response.getData());
    }

    @PostMapping("/captcha/check")
    public ApiResponse<Map<String, String>> checkCaptcha(@Valid @RequestBody CaptchaCheckRequest request) {
        cloud.tianai.captcha.common.response.ApiResponse<?> response = captchaService.matching(request.id(), request.data());
        if (!response.isSuccess()) {
            return ApiResponse.fail(response.getCode(), response.getMsg());
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
    public ApiResponse<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.login(request, servletRequest));
    }

    @GetMapping(value = "/dev-login", produces = MediaType.TEXT_HTML_VALUE)
    public String devLogin(@RequestParam(defaultValue = "1") Long userId, HttpServletRequest servletRequest) throws JsonProcessingException {
        AuthTokenResponse response = authService.devLoginAs(userId, servletRequest);
        return """
                <!doctype html>
                <meta charset="utf-8">
                <title>本地开发登录</title>
                <script>
                localStorage.setItem('token', %s);
                localStorage.setItem('user', JSON.stringify(%s));
                localStorage.setItem('permissions', JSON.stringify(%s));
                location.replace('/dashboard');
                </script>
                """.formatted(
                objectMapper.writeValueAsString(response.token()),
                objectMapper.writeValueAsString(response.user()),
                objectMapper.writeValueAsString(response.permissions())
        );
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<SysUser> me() {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(userMapper.selectById(currentUser.userId()));
    }
}
