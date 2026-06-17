package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;

import java.util.regex.Pattern;

public final class UserInputValidator {
    private static final Pattern REAL_NAME_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5·]{2,10}$");
    private static final Pattern MAINLAND_PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private UserInputValidator() {
    }

    public static String normalizeRegisterRealName(String realName) {
        String normalized = trim(realName);
        if (!REAL_NAME_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException("真实姓名需为2-10位中文或中间点");
        }
        return normalized;
    }

    public static String normalizeRegisterPhone(String phone) {
        String normalized = trim(phone);
        if (!MAINLAND_PHONE_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException("请输入正确的手机号");
        }
        return normalized;
    }

    public static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
