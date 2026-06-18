package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.dto.AuthSessionResponse;
import cn.datong.standard.dto.AuthTokenResponse;
import cn.datong.standard.dto.AuthUser;
import cn.datong.standard.dto.LoginRequest;
import cn.datong.standard.dto.RegisterRequest;
import cn.datong.standard.entity.SysRegisterApproval;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.mapper.SysRegisterApprovalMapper;
import cn.datong.standard.mapper.SysUserMapper;
import cn.datong.standard.security.JwtTokenProvider;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final SysUserMapper userMapper;
    private final SysRegisterApprovalMapper approvalMapper;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PermissionService permissionService;
    private final OperationLogService logService;
    private final OrgAssignmentService orgAssignmentService;
    @Autowired(required = false)
    private LoginAttemptService loginAttemptService;

    public void register(RegisterRequest request) {
        captchaService.verify(request.captchaKey(), request.captchaCode());
        String realName = UserInputValidator.normalizeRegisterRealName(request.realName());
        String phone = UserInputValidator.normalizeRegisterPhone(request.phone());
        PasswordPolicy.validate(request.password(), request.confirmPassword());
        orgAssignmentService.requireAssignableDept(request.deptId());
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhone, phone));
        if (count > 0) {
            throw new BusinessException("手机号已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(phone);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setDeptId(request.deptId());
        user.setStatus("DISABLED");
        user.setApprovalStatus("PENDING");
        user.setIsSuperAdmin(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeleted(0);
        userMapper.insert(user);

        SysRegisterApproval approval = new SysRegisterApproval();
        approval.setUserId(user.getId());
        approval.setApprovalStatus("PENDING");
        approval.setCreatedAt(LocalDateTime.now());
        approvalMapper.insert(approval);
    }

    public AuthTokenResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        captchaService.verify(request.captchaKey(), request.captchaCode());
        String phone = UserInputValidator.trim(request.phone());
        if (loginAttemptService != null) {
            loginAttemptService.assertAllowed(phone, servletRequest);
        }
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhone, phone));
        if (user == null || Integer.valueOf(1).equals(user.getDeleted()) || !passwordEncoder.matches(request.password(), user.getPassword())) {
            if (loginAttemptService != null) {
                loginAttemptService.recordFailure(phone, servletRequest);
            }
            logService.login(phone, null, "FAIL", "手机号或密码错误", servletRequest);
            throw new BusinessException("手机号或密码错误");
        }
        if (!"APPROVED".equals(user.getApprovalStatus()) || !"ENABLED".equals(user.getStatus())) {
            String reason = loginBlockedMessage(user);
            if (loginAttemptService != null) {
                loginAttemptService.recordFailure(phone, servletRequest);
            }
            logService.login(phone, user.getId(), "FAIL", reason, servletRequest);
            throw new BusinessException(reason);
        }
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
        String token = jwtTokenProvider.createToken(user.getId());
        Set<String> permissions = permissionService.getEffectivePermissions(user.getId(), Boolean.TRUE.equals(user.getIsSuperAdmin()));
        if (loginAttemptService != null) {
            loginAttemptService.clear(phone, servletRequest);
        }
        logService.login(phone, user.getId(), "SUCCESS", null, servletRequest);
        return new AuthTokenResponse(token, authUser(user), permissions);
    }

    public AuthUser currentUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return authUser(user);
    }

    public AuthSessionResponse currentSession(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return new AuthSessionResponse(authUser(user),
                permissionService.getEffectivePermissions(user.getId(), Boolean.TRUE.equals(user.getIsSuperAdmin())));
    }

    private AuthUser authUser(SysUser user) {
        boolean superAdmin = Boolean.TRUE.equals(user.getIsSuperAdmin());
        Set<Long> adminUserIds = superAdmin ? Set.of() : orgAssignmentService.adminUserIds();
        boolean admin = superAdmin || adminUserIds != null && adminUserIds.contains(user.getId());
        return AuthUser.of(user, admin);
    }

    private String loginBlockedMessage(SysUser user) {
        if ("PENDING".equals(user.getApprovalStatus())) {
            return "管理员审核中";
        }
        if ("REJECTED".equals(user.getApprovalStatus())) {
            return "注册申请已被拒绝，请联系管理员";
        }
        if (!"ENABLED".equals(user.getStatus())) {
            return "账号已禁用，请联系管理员";
        }
        return "用户未审批或已禁用";
    }
}
