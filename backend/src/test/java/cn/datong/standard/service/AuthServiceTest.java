package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.dto.AuthTokenResponse;
import cn.datong.standard.dto.RegisterRequest;
import cn.datong.standard.entity.SysRegisterApproval;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.mapper.SysRegisterApprovalMapper;
import cn.datong.standard.mapper.SysUserMapper;
import cn.datong.standard.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
    void devLoginAsSpecificUserAllowsLoopbackRequest() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        PermissionService permissionService = mock(PermissionService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        SysUser user = new SysUser();
        user.setId(42L);
        user.setDeptId(7L);
        user.setIsSuperAdmin(false);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(userMapper.selectById(42L)).thenReturn(user);
        when(jwtTokenProvider.createToken(42L, 7L, false)).thenReturn("dev-token-42");
        when(permissionService.getEffectivePermissions(42L, false)).thenReturn(Set.of("file:upload"));
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

        AuthTokenResponse response = service.devLoginAs(42L, request);

        assertThat(response.token()).isEqualTo("dev-token-42");
        assertThat(response.user()).isSameAs(user);
        assertThat(response.permissions()).containsExactly("file:upload");
    }

    @Test
    void devLoginAsSpecificUserRejectsNonLocalRequest() {
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

        assertThatThrownBy(() -> service.devLoginAs(42L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("仅本地开发环境允许跳过登录");
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

    @Test
    void registerCreatesDisabledPendingUserAndPendingApproval() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRegisterApprovalMapper approvalMapper = mock(SysRegisterApprovalMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        CaptchaService captchaService = mock(CaptchaService.class);
        OrgAssignmentService orgAssignmentService = mock(OrgAssignmentService.class);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("Password123")).thenReturn("encoded-password");
        doAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(100L);
            return 1;
        }).when(userMapper).insert(any(SysUser.class));
        AuthService service = new AuthService(
                userMapper,
                approvalMapper,
                passwordEncoder,
                captchaService,
                mock(JwtTokenProvider.class),
                mock(PermissionService.class),
                mock(OperationLogService.class),
                orgAssignmentService
        );

        service.register(new RegisterRequest(
                "权限测试_注册用户",
                "Password123",
                "权限测试_注册用户",
                "13800000000",
                25L,
                "captcha-id",
                CaptchaService.SLIDER_PASSED_CODE
        ));

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        ArgumentCaptor<SysRegisterApproval> approvalCaptor = ArgumentCaptor.forClass(SysRegisterApproval.class);
        verify(captchaService).verify("captcha-id", CaptchaService.SLIDER_PASSED_CODE);
        verify(orgAssignmentService).requireAssignableDept(25L);
        verify(userMapper).insert(userCaptor.capture());
        verify(approvalMapper).insert(approvalCaptor.capture());
        SysUser user = userCaptor.getValue();
        assertThat(user.getUsername()).isEqualTo("权限测试_注册用户");
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        assertThat(user.getDeptId()).isEqualTo(25L);
        assertThat(user.getStatus()).isEqualTo("DISABLED");
        assertThat(user.getApprovalStatus()).isEqualTo("PENDING");
        assertThat(user.getIsSuperAdmin()).isFalse();
        SysRegisterApproval approval = approvalCaptor.getValue();
        assertThat(approval.getUserId()).isEqualTo(100L);
        assertThat(approval.getApprovalStatus()).isEqualTo("PENDING");
        assertThat(approval.getCreatedAt()).isNotNull();
    }

    @Test
    void pendingRegisteredUserCannotLoginBeforeApproval() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        SysUser user = new SysUser();
        user.setId(100L);
        user.setUsername("权限测试_注册用户");
        user.setPassword("encoded-password");
        user.setStatus("DISABLED");
        user.setApprovalStatus("PENDING");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("Password123", "encoded-password")).thenReturn(true);
        AuthService service = new AuthService(
                userMapper,
                mock(SysRegisterApprovalMapper.class),
                passwordEncoder,
                mock(CaptchaService.class),
                mock(JwtTokenProvider.class),
                mock(PermissionService.class),
                mock(OperationLogService.class),
                mock(OrgAssignmentService.class)
        );

        assertThatThrownBy(() -> service.login(
                new cn.datong.standard.dto.LoginRequest(
                        "权限测试_注册用户",
                        "Password123",
                        "captcha-id",
                        CaptchaService.SLIDER_PASSED_CODE
                ),
                null
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户未审批或已禁用");
    }
}
