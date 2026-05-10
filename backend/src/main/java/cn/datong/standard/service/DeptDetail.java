package cn.datong.standard.service;

import cn.datong.standard.dto.UserView;
import cn.datong.standard.entity.SysDept;

import java.util.List;

public record DeptDetail(
        SysDept dept,
        boolean assignable,
        String assignableMessage,
        List<UserView> users,
        List<UserView> admins
) {
}
