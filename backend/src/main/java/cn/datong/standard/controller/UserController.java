package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.dto.UserView;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.entity.SysUserPermission;
import cn.datong.standard.entity.SysUserRole;
import cn.datong.standard.mapper.SysUserMapper;
import cn.datong.standard.mapper.SysUserPermissionMapper;
import cn.datong.standard.mapper.SysUserRoleMapper;
import cn.datong.standard.security.SecurityUtils;
import cn.datong.standard.service.OrgAssignmentService;
import cn.datong.standard.service.PermissionService;
import cn.datong.standard.service.UserAdminService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserPermissionMapper userPermissionMapper;
    private final PermissionService permissionService;
    private final UserAdminService userAdminService;
    private final OrgAssignmentService orgAssignmentService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ApiResponse<List<UserView>> list(@RequestParam(required = false) Long deptId,
                                            @RequestParam(required = false) String keyword) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        permissionService.require(currentUser.userId(), currentUser.superAdmin(), "user:view");
        return ApiResponse.success(userAdminService.listUserViews(deptId, keyword, currentUser));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserView> detail(@PathVariable Long id) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        permissionService.require(currentUser.userId(), currentUser.superAdmin(), "user:view");
        return ApiResponse.success(userAdminService.userView(id, currentUser));
    }

    @PostMapping
    public ApiResponse<SysUser> create(@RequestBody SysUser user) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        requireUserManager(currentUser);
        userAdminService.requireCreateInManagedDept(currentUser, user.getDeptId());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus("ENABLED");
        user.setApprovalStatus("APPROVED");
        user.setIsSuperAdmin(Boolean.TRUE.equals(user.getIsSuperAdmin()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeleted(0);
        userMapper.insert(user);
        return ApiResponse.success(user);
    }

    @PutMapping("/{id}")
    public ApiResponse<SysUser> update(@PathVariable Long id, @RequestBody SysUser user) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        requireUserManager(currentUser);
        userAdminService.requireSameDeptManage(currentUser, id);
        if (user.getDeptId() != null) {
            userAdminService.requireCreateInManagedDept(currentUser, user.getDeptId());
        }
        user.setId(id);
        user.setPassword(null);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return ApiResponse.success(userMapper.selectById(id));
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        setStatus(id, "DISABLED");
        return ApiResponse.success();
    }

    @PostMapping("/{id}/enable")
    public ApiResponse<Void> enable(@PathVariable Long id) {
        setStatus(id, "ENABLED");
        return ApiResponse.success();
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @RequestParam String password) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        requireUserManager(currentUser);
        userAdminService.requireSameDeptManage(currentUser, id);
        SysUser user = userMapper.selectById(id);
        user.setPassword(passwordEncoder.encode(password));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/roles")
    public ApiResponse<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        requireSuperAdmin(currentUser);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        for (Long roleId : roleIds) {
            SysUserRole row = new SysUserRole();
            row.setUserId(id);
            row.setRoleId(roleId);
            userRoleMapper.insert(row);
        }
        return ApiResponse.success();
    }

    @PostMapping("/{id}/promote-admin")
    public ApiResponse<Void> promoteAdmin(@PathVariable Long id) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        userAdminService.promoteAdmin(id, currentUser.superAdmin());
        return ApiResponse.success();
    }

    @PostMapping("/{id}/demote-admin")
    public ApiResponse<Void> demoteAdmin(@PathVariable Long id) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        userAdminService.demoteAdmin(id, currentUser.superAdmin());
        return ApiResponse.success();
    }

    @PostMapping("/{id}/permissions")
    public ApiResponse<Void> savePermissions(@PathVariable Long id, @RequestBody List<SysUserPermission> permissions) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        requireSuperAdmin(currentUser);
        userPermissionMapper.delete(new LambdaQueryWrapper<SysUserPermission>().eq(SysUserPermission::getUserId, id));
        permissions.forEach(item -> {
            item.setUserId(id);
            item.setCreatedBy(currentUser.userId());
            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
            userPermissionMapper.insert(item);
        });
        return ApiResponse.success();
    }

    @GetMapping("/{id}/effective-permissions")
    public ApiResponse<Set<String>> effectivePermissions(@PathVariable Long id) {
        SysUser user = userMapper.selectById(id);
        return ApiResponse.success(permissionService.getEffectivePermissions(id, Boolean.TRUE.equals(user.getIsSuperAdmin())));
    }

    private void setStatus(Long id, String status) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        requireUserManager(currentUser);
        userAdminService.requireSameDeptManage(currentUser, id);
        SysUser user = userMapper.selectById(id);
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private void requireSuperAdmin(CurrentUser currentUser) {
        if (!currentUser.superAdmin()) {
            throw new cn.datong.standard.common.BusinessException(403, "只有超级管理员可以管理用户");
        }
    }

    private void requireUserManager(CurrentUser currentUser) {
        if (currentUser.superAdmin()) {
            return;
        }
        userAdminService.requireAdminUser(currentUser.userId());
    }
}
