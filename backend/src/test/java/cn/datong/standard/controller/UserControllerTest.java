package cn.datong.standard.controller;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.dto.ResetPasswordRequest;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.mapper.SysUserMapper;
import cn.datong.standard.mapper.SysUserPermissionMapper;
import cn.datong.standard.mapper.SysUserRoleMapper;
import cn.datong.standard.service.OperationLogService;
import cn.datong.standard.service.OrgAssignmentService;
import cn.datong.standard.service.PermissionService;
import cn.datong.standard.service.UserAdminService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resetPasswordAcceptsJsonBodyAndWritesEncodedPasswordAndLog() {
        authenticateAsSuperAdmin();
        SysUserMapper userMapper = mock(SysUserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        OperationLogService logService = mock(OperationLogService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        SysUser user = user(50L);
        when(userMapper.selectById(50L)).thenReturn(user);
        when(passwordEncoder.encode("123456")).thenReturn("encoded-123456");
        UserController controller = controller(userMapper, passwordEncoder, logService);

        controller.resetPassword(50L, new ResetPasswordRequest("123456"), null, request);

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-123456");
        assertThat(userCaptor.getValue().getUpdatedAt()).isNotNull();
        verify(logService).operation(1L, "重置密码", "USER", 50L, "SUCCESS", null, request);
    }

    @Test
    void resetPasswordRejectsShortPassword() {
        authenticateAsSuperAdmin();
        SysUserMapper userMapper = mock(SysUserMapper.class);
        UserController controller = controller(userMapper, mock(PasswordEncoder.class), mock(OperationLogService.class));

        assertThatThrownBy(() -> controller.resetPassword(50L, new ResetPasswordRequest("12345"), null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("密码长度需为 6-64 位");

        verify(userMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    void resetPasswordRejectsMissingUser() {
        authenticateAsSuperAdmin();
        SysUserMapper userMapper = mock(SysUserMapper.class);
        when(userMapper.selectById(50L)).thenReturn(null);
        UserController controller = controller(userMapper, mock(PasswordEncoder.class), mock(OperationLogService.class));

        assertThatThrownBy(() -> controller.resetPassword(50L, new ResetPasswordRequest("123456"), null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不存在");

        verify(userMapper, never()).updateById(any(SysUser.class));
    }

    private UserController controller(SysUserMapper userMapper, PasswordEncoder passwordEncoder, OperationLogService logService) {
        return new UserController(
                userMapper,
                mock(SysUserRoleMapper.class),
                mock(SysUserPermissionMapper.class),
                mock(PermissionService.class),
                mock(UserAdminService.class),
                mock(OrgAssignmentService.class),
                passwordEncoder,
                logService
        );
    }

    private void authenticateAsSuperAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new CurrentUser(1L, 1L, true), null, List.of())
        );
    }

    private SysUser user(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername("q1");
        return user;
    }
}
