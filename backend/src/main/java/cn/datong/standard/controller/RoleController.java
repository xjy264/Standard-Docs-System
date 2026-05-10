package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.entity.SysPermission;
import cn.datong.standard.entity.SysRole;
import cn.datong.standard.entity.SysRolePermission;
import cn.datong.standard.mapper.SysPermissionMapper;
import cn.datong.standard.mapper.SysRoleMapper;
import cn.datong.standard.mapper.SysRolePermissionMapper;
import cn.datong.standard.security.SecurityUtils;
import cn.datong.standard.service.PermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final PermissionService permissionService;

    @GetMapping
    public ApiResponse<List<SysRole>> list() {
        CurrentUser currentUser = SecurityUtils.currentUser();
        permissionService.require(currentUser.userId(), currentUser.superAdmin(), "role:view");
        return ApiResponse.success(roleMapper.selectList(new LambdaQueryWrapper<SysRole>().eq(SysRole::getDeleted, 0)));
    }

    @PostMapping
    public ApiResponse<SysRole> create(@RequestBody SysRole role) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        permissionService.require(currentUser.userId(), currentUser.superAdmin(), "role:manage");
        role.setIsSystem(Boolean.FALSE);
        role.setStatus("ENABLED");
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        role.setDeleted(0);
        roleMapper.insert(role);
        return ApiResponse.success(role);
    }

    @PutMapping("/{id}")
    public ApiResponse<SysRole> update(@PathVariable Long id, @RequestBody SysRole role) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        permissionService.require(currentUser.userId(), currentUser.superAdmin(), "role:manage");
        role.setId(id);
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.updateById(role);
        return ApiResponse.success(roleMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        permissionService.require(currentUser.userId(), currentUser.superAdmin(), "role:manage");
        roleMapper.deleteById(id);
        return ApiResponse.success();
    }

    @GetMapping("/permissions")
    public ApiResponse<List<SysPermission>> permissions() {
        return ApiResponse.success(permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getStatus, "ENABLED")
                .orderByAsc(SysPermission::getSortOrder)));
    }

    @PostMapping("/{id}/permissions")
    public ApiResponse<Void> savePermissions(@PathVariable Long id, @RequestBody List<String> permissionCodes) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        permissionService.require(currentUser.userId(), currentUser.superAdmin(), "role:manage");
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
        for (String code : permissionCodes) {
            SysRolePermission row = new SysRolePermission();
            row.setRoleId(id);
            row.setPermissionCode(code);
            rolePermissionMapper.insert(row);
        }
        return ApiResponse.success();
    }
}
