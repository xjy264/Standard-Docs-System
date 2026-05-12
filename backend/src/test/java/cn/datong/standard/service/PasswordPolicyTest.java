package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @Test
    void acceptsStrongPasswordWhenConfirmationMatches() {
        assertThatCode(() -> PasswordPolicy.validate("Password123!", "Password123!")).doesNotThrowAnyException();
    }

    @Test
    void rejectsPasswordWithInvalidLength() {
        assertThatThrownBy(() -> PasswordPolicy.validate("Aa1!", "Aa1!"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("密码长度需为 8-20 位");
    }

    @Test
    void reportsMissingPasswordRules() {
        assertThatThrownBy(() -> PasswordPolicy.validate("password", "password"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("密码缺少大写字母、数字、特殊符号");
    }

    @Test
    void rejectsMismatchedConfirmation() {
        assertThatThrownBy(() -> PasswordPolicy.validate("Password123!", "Password123@"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("两次输入的密码不一致");
    }
}
