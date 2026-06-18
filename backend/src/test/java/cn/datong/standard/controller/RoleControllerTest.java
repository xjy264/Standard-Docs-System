package cn.datong.standard.controller;

import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.mapper.SysPermissionMapper;
import cn.datong.standard.mapper.SysRoleMapper;
import cn.datong.standard.mapper.SysRolePermissionMapper;
import cn.datong.standard.service.PermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RoleControllerTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void permissionsRequiresRoleViewPermission() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new CurrentUser(3L, 2L, false), null, List.of())
        );
        PermissionService permissionService = mock(PermissionService.class);
        RoleController controller = new RoleController(
                mock(SysRoleMapper.class),
                mock(SysPermissionMapper.class),
                mock(SysRolePermissionMapper.class),
                permissionService
        );

        controller.permissions();

        verify(permissionService).require(3L, false, "role:view");
    }
}
