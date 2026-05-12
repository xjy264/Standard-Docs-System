package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.dto.AuthTokenResponse;
import cn.datong.standard.dto.LoginRequest;
import cn.datong.standard.dto.RegisterRequest;
import cn.datong.standard.entity.SysRegisterApproval;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.mapper.SysRegisterApprovalMapper;
import cn.datong.standard.mapper.SysUserMapper;
import cn.datong.standard.security.JwtTokenProvider;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
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

    public void register(RegisterRequest request) {
        captchaService.verify(request.captchaKey(), request.captchaCode());
        PasswordPolicy.validate(request.password(), request.confirmPassword());
        orgAssignmentService.requireAssignableDept(request.deptId());
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.username()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRealName(request.realName());
        user.setPhone(request.phone());
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
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            logService.login(request.username(), null, "FAIL", "用户名或密码错误", servletRequest);
            throw new BusinessException("用户名或密码错误");
        }
        if (!"APPROVED".equals(user.getApprovalStatus()) || !"ENABLED".equals(user.getStatus())) {
            logService.login(request.username(), user.getId(), "FAIL", "用户未审批或已禁用", servletRequest);
            throw new BusinessException("用户未审批或已禁用");
        }
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
        String token = jwtTokenProvider.createToken(user.getId(), user.getDeptId(), Boolean.TRUE.equals(user.getIsSuperAdmin()));
        Set<String> permissions = permissionService.getEffectivePermissions(user.getId(), Boolean.TRUE.equals(user.getIsSuperAdmin()));
        logService.login(request.username(), user.getId(), "SUCCESS", null, servletRequest);
        return new AuthTokenResponse(token, user, permissions);
    }

    public AuthTokenResponse devLoginAsUserOne(HttpServletRequest servletRequest) {
        return devLoginAs(1L, servletRequest);
    }

    public AuthTokenResponse devLoginAs(Long userId, HttpServletRequest servletRequest) {
        if (!isLocalRequest(servletRequest)) {
            throw new BusinessException(403, "仅本地开发环境允许跳过登录");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("本地开发用户不存在：id=" + userId);
        }
        String token = jwtTokenProvider.createToken(user.getId(), user.getDeptId(), Boolean.TRUE.equals(user.getIsSuperAdmin()));
        Set<String> permissions = permissionService.getEffectivePermissions(user.getId(), Boolean.TRUE.equals(user.getIsSuperAdmin()));
        return new AuthTokenResponse(token, user, permissions);
    }

    private boolean isLocalRequest(HttpServletRequest servletRequest) {
        try {
            String remoteAddr = servletRequest.getRemoteAddr();
            return remoteAddr != null && InetAddress.getByName(remoteAddr).isLoopbackAddress();
        } catch (Exception ignored) {
            return false;
        }
    }
}
