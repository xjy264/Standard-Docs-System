package cn.datong.standard.common;

import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.entity.SysErrorEvent;
import cn.datong.standard.security.SecurityUtils;
import cn.datong.standard.service.ErrorEventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final ErrorEventService errorEventService;

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException ex, HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(httpStatus(ex.getCode()));
        String message = normalizeMessage(ex.getMessage());
        if (ex.getCode() >= 500 || ex.getCode() == 403) {
            SysErrorEvent event = record(ex, request, ex.getCode(), ex.getCode(), message);
            if (event != null && ex.getCode() >= 500) {
                message = message + " 错误编号：" + event.getErrorId();
            }
        }
        return ApiResponse.fail(ex.getCode(), message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValid(MethodArgumentNotValidException ex, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(this::validationMessage)
                .orElse("参数校验失败");
        return ApiResponse.fail(400, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraint(ConstraintViolationException ex, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return ApiResponse.fail(400, "参数校验失败，请检查填写内容。");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String message = "系统服务暂时不可用，请稍后重试或联系管理员。";
        SysErrorEvent event = record(ex, request, 500, 500, message);
        if (event != null) {
            message = message + " 错误编号：" + event.getErrorId();
            log.error("未处理的系统异常，errorId={}，exceptionClass={}", event.getErrorId(), ex.getClass().getName());
        } else {
            log.error("未处理的系统异常，exceptionClass={}", ex.getClass().getName());
        }
        return ApiResponse.fail(500, message);
    }

    private SysErrorEvent record(Exception ex, HttpServletRequest request, int statusCode, int businessCode, String message) {
        CurrentUser currentUser = currentUserOrNull();
        return errorEventService.recordBackendException(
                ex,
                request,
                statusCode,
                businessCode,
                message,
                currentUser == null ? null : currentUser.userId(),
                currentUser == null ? null : currentUser.deptId()
        );
    }

    private CurrentUser currentUserOrNull() {
        try {
            return SecurityUtils.currentUser();
        } catch (Exception ignored) {
            return null;
        }
    }

    private int httpStatus(int code) {
        if (code >= 400 && code <= 599) {
            return code;
        }
        return HttpServletResponse.SC_BAD_REQUEST;
    }

    private String validationMessage(FieldError error) {
        String label = fieldLabel(error.getField());
        String code = error.getCode();
        if ("NotBlank".equals(code) || "NotNull".equals(code) || "NotEmpty".equals(code)) {
            return label + "不能为空";
        }
        String defaultMessage = error.getDefaultMessage();
        if (defaultMessage != null && !defaultMessage.isBlank() && !defaultMessage.matches(".*[A-Za-z].*")) {
            return label + defaultMessage;
        }
        return label + "填写不正确";
    }

    private String fieldLabel(String field) {
        return switch (field) {
            case "phone" -> "手机号";
            case "password" -> "密码";
            case "confirmPassword" -> "确认密码";
            case "realName" -> "真实姓名";
            case "deptId" -> "所属组织";
            case "captchaKey", "captchaCode", "id", "data" -> "人机验证";
            default -> "填写内容";
        };
    }

    private String normalizeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "操作未完成，请稍后重试。";
        }
        if (message.startsWith("没有权限：")) {
            return "当前账号没有该功能权限，请联系管理员开通。";
        }
        if (message.startsWith("系统异常")) {
            return "系统服务暂时不可用，请稍后重试或联系管理员。";
        }
        if (message.startsWith("角色不存在：")) {
            return "角色配置不存在，请联系管理员处理。";
        }
        if (message.contains("MinIO") || message.contains("9000") || message.contains("Connection refused")
                || message.contains("Failed to connect")) {
            return "文件存储服务暂时不可用，请稍后重试或联系管理员。";
        }
        return message;
    }
}
