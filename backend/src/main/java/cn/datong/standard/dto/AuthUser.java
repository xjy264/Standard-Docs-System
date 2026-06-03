package cn.datong.standard.dto;

import cn.datong.standard.entity.SysUser;

public record AuthUser(
        Long id,
        String username,
        String realName,
        String phone,
        Long deptId,
        Boolean isSuperAdmin,
        boolean admin
) {
    public static AuthUser of(SysUser user, boolean admin) {
        return new AuthUser(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getPhone(),
                user.getDeptId(),
                user.getIsSuperAdmin(),
                admin
        );
    }
}
