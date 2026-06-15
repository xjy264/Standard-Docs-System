package cn.datong.standard.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException ex) {
        return ApiResponse.fail(ex.getCode(), normalizeMessage(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(this::validationMessage)
                .orElse("参数校验失败");
        return ApiResponse.fail(400, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraint(ConstraintViolationException ex) {
        return ApiResponse.fail(400, "参数校验失败，请检查填写内容。");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("未处理的系统异常", ex);
        return ApiResponse.fail(500, "系统服务暂时不可用，请稍后重试或联系管理员。");
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
