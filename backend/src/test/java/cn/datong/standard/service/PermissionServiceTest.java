package cn.datong.standard.service;

import cn.datong.standard.entity.SysPermissionGrant;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionServiceTest {

    @Test
    void denyGrantOverridesRoleAllowGrant() {
        PermissionService permissionService = new PermissionService();

        boolean result = permissionService.hasPermission(
                false,
                "file:delete",
                List.of("file:delete", "file:upload"),
                List.of(new SysPermissionGrant("file:delete", "deny"))
        );

        assertThat(result).isFalse();
    }

    @Test
    void superAdminOwnsEverySystemPermission() {
        PermissionService permissionService = new PermissionService();

        boolean result = permissionService.hasPermission(
                true,
                "system:manage",
                List.of(),
                List.of()
        );

        assertThat(result).isTrue();
    }
}
