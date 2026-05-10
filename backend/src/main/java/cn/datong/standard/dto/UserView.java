package cn.datong.standard.dto;

import cn.datong.standard.entity.SysUser;

public record UserView(
        Long id,
        String username,
        String realName,
        String phone,
        Long deptId,
        String status,
        String approvalStatus,
        Boolean isSuperAdmin,
        String deptName,
        String identity,
        boolean admin
) {
    public static UserView of(SysUser user, String identity, boolean admin) {
        return of(user, identity, admin, null);
    }

    public static UserView of(SysUser user, String identity, boolean admin, String deptName) {
        return new UserView(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getPhone(),
                user.getDeptId(),
                user.getStatus(),
                user.getApprovalStatus(),
                user.getIsSuperAdmin(),
                deptName,
                identity,
                admin
        );
    }
}
