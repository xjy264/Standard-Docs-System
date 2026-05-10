package cn.datong.standard.service;

import cn.datong.standard.entity.SysPermissionGrant;
import cn.datong.standard.entity.SysRolePermission;
import cn.datong.standard.entity.SysUserPermission;
import cn.datong.standard.entity.SysUserRole;
import cn.datong.standard.mapper.SysRolePermissionMapper;
import cn.datong.standard.mapper.SysUserPermissionMapper;
import cn.datong.standard.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PermissionService {
    private SysUserRoleMapper userRoleMapper;
    private SysRolePermissionMapper rolePermissionMapper;
    private SysUserPermissionMapper userPermissionMapper;

    public PermissionService() {
    }

    @Autowired
    public PermissionService(SysUserRoleMapper userRoleMapper, SysRolePermissionMapper rolePermissionMapper,
                             SysUserPermissionMapper userPermissionMapper) {
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userPermissionMapper = userPermissionMapper;
    }

    public boolean hasPermission(boolean superAdmin, String permissionCode, List<String> rolePermissions,
                                 List<SysPermissionGrant> userGrants) {
        if (superAdmin) {
            return true;
        }
        boolean allow = rolePermissions.contains(permissionCode);
        for (SysPermissionGrant grant : userGrants) {
            if (!permissionCode.equals(grant.permissionCode())) {
                continue;
            }
            if ("deny".equalsIgnoreCase(grant.effect())) {
                return false;
            }
            if ("allow".equalsIgnoreCase(grant.effect())) {
                allow = true;
            }
        }
        return allow;
    }

    public boolean hasPermission(Long userId, boolean superAdmin, String permissionCode) {
        return getEffectivePermissions(userId, superAdmin).contains(permissionCode);
    }

    public Set<String> getEffectivePermissions(Long userId, boolean superAdmin) {
        if (superAdmin) {
            return Set.of("*");
        }
        Set<String> rolePermissions = new HashSet<>();
        List<SysUserRole> roles = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
        for (SysUserRole role : roles) {
            rolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>()
                    .eq(SysRolePermission::getRoleId, role.getRoleId()))
                    .forEach(item -> rolePermissions.add(item.getPermissionCode()));
        }

        Set<String> denies = new HashSet<>();
        Set<String> allows = new HashSet<>(rolePermissions);
        userPermissionMapper.selectList(new LambdaQueryWrapper<SysUserPermission>()
                .eq(SysUserPermission::getUserId, userId))
                .forEach(item -> {
                    if ("deny".equalsIgnoreCase(item.getEffect())) {
                        denies.add(item.getPermissionCode());
                    } else if ("allow".equalsIgnoreCase(item.getEffect())) {
                        allows.add(item.getPermissionCode());
                    }
                });
        allows.removeAll(denies);
        return allows;
    }

    public void require(Long userId, boolean superAdmin, String permissionCode) {
        if (!hasPermission(userId, superAdmin, permissionCode) && !superAdmin) {
            throw new cn.datong.standard.common.BusinessException(403, "没有权限：" + permissionCode);
        }
    }
}
