package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.dto.AuthTokenResponse;
import cn.datong.standard.dto.RegisterRequest;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.mapper.SysRegisterApprovalMapper;
import cn.datong.standard.mapper.SysUserMapper;
import cn.datong.standard.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void devLoginAsUserOneAllowsLoopbackRequest() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        PermissionService permissionService = mock(PermissionService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        SysUser user = new SysUser();
        user.setId(1L);
        user.setDeptId(1L);
        user.setIsSuperAdmin(true);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(jwtTokenProvider.createToken(1L, 1L, true)).thenReturn("dev-token");
        when(permissionService.getEffectivePermissions(1L, true)).thenReturn(Set.of("*"));
        AuthService service = new AuthService(
                userMapper,
                mock(SysRegisterApprovalMapper.class),
                mock(PasswordEncoder.class),
                mock(CaptchaService.class),
                jwtTokenProvider,
                permissionService,
                mock(OperationLogService.class),
                mock(OrgAssignmentService.class)
        );

        AuthTokenResponse response = service.devLoginAsUserOne(request);

        assertThat(response.token()).isEqualTo("dev-token");
        assertThat(response.user()).isSameAs(user);
        assertThat(response.permissions()).containsExactly("*");
    }

    @Test
    void devLoginAsUserOneRejectsNonLocalRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.10");
        AuthService service = new AuthService(
                mock(SysUserMapper.class),
                mock(SysRegisterApprovalMapper.class),
                mock(PasswordEncoder.class),
                mock(CaptchaService.class),
                mock(JwtTokenProvider.class),
                mock(PermissionService.class),
                mock(OperationLogService.class),
                mock(OrgAssignmentService.class)
        );

        assertThatThrownBy(() -> service.devLoginAsUserOne(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("仅本地开发环境允许跳过登录");
    }

    @Test
    void registerRejectsUnassignableDept() {
        OrgAssignmentService orgAssignmentService = mock(OrgAssignmentService.class);
        org.mockito.Mockito.doThrow(new BusinessException("该组织不能直接配置用户，请选择具体科室或车间"))
                .when(orgAssignmentService).requireAssignableDept(24L);
        AuthService service = new AuthService(
                mock(SysUserMapper.class),
                mock(SysRegisterApprovalMapper.class),
                mock(PasswordEncoder.class),
                mock(CaptchaService.class),
                mock(JwtTokenProvider.class),
                mock(PermissionService.class),
                mock(OperationLogService.class),
                orgAssignmentService
        );

        assertThatThrownBy(() -> service.register(new RegisterRequest(
                "zhangsan",
                "Password123",
                "张三",
                "13800000000",
                24L,
                "captcha-id",
                CaptchaService.SLIDER_PASSED_CODE
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该组织不能直接配置用户，请选择具体科室或车间");
    }
}
