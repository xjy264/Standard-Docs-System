package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.entity.SysPermission;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.entity.SysUserPermission;
import cn.datong.standard.mapper.SysPermissionMapper;
import cn.datong.standard.mapper.SysUserMapper;
import cn.datong.standard.mapper.SysUserPermissionMapper;
import cn.datong.standard.security.SecurityUtils;
import cn.datong.standard.service.PermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/permission-matrix")
@RequiredArgsConstructor
public class PermissionMatrixController {
    private final SysUserMapper userMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysUserPermissionMapper userPermissionMapper;
    private final PermissionService permissionService;

    @GetMapping
    public ApiResponse<Map<String, Object>> matrix(@RequestParam Long deptId) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        permissionService.require(currentUser.userId(), currentUser.superAdmin(), "role:manage");
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, deptId));
        List<SysPermission> permissions = permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getStatus, "ENABLED")
                .orderByAsc(SysPermission::getSortOrder));
        Map<Long, Set<String>> effective = new LinkedHashMap<>();
        for (SysUser user : users) {
            effective.put(user.getId(), permissionService.getEffectivePermissions(user.getId(), Boolean.TRUE.equals(user.getIsSuperAdmin())));
        }
        return ApiResponse.success(Map.of("users", users, "permissions", permissions, "effective", effective));
    }

    @PostMapping("/users/{userId}")
    public ApiResponse<Void> saveUserMatrix(@PathVariable Long userId, @RequestBody List<SysUserPermission> permissions) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        permissionService.require(currentUser.userId(), currentUser.superAdmin(), "role:manage");
        userPermissionMapper.delete(new LambdaQueryWrapper<SysUserPermission>().eq(SysUserPermission::getUserId, userId));
        for (SysUserPermission permission : permissions) {
            permission.setUserId(userId);
            permission.setCreatedBy(currentUser.userId());
            permission.setCreatedAt(LocalDateTime.now());
            permission.setUpdatedAt(LocalDateTime.now());
            userPermissionMapper.insert(permission);
        }
        return ApiResponse.success();
    }

    @PostMapping("/copy")
    public ApiResponse<Void> copy(@RequestParam Long sourceUserId, @RequestParam Long targetUserId) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        permissionService.require(currentUser.userId(), currentUser.superAdmin(), "role:manage");
        List<SysUserPermission> source = userPermissionMapper.selectList(new LambdaQueryWrapper<SysUserPermission>()
                .eq(SysUserPermission::getUserId, sourceUserId));
        userPermissionMapper.delete(new LambdaQueryWrapper<SysUserPermission>().eq(SysUserPermission::getUserId, targetUserId));
        for (SysUserPermission permission : source) {
            SysUserPermission copied = new SysUserPermission();
            copied.setUserId(targetUserId);
            copied.setPermissionCode(permission.getPermissionCode());
            copied.setEffect(permission.getEffect());
            copied.setCreatedBy(currentUser.userId());
            copied.setCreatedAt(LocalDateTime.now());
            copied.setUpdatedAt(LocalDateTime.now());
            userPermissionMapper.insert(copied);
        }
        return ApiResponse.success();
    }
}
