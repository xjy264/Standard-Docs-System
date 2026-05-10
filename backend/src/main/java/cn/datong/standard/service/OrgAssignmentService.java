package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.dto.UserView;
import cn.datong.standard.entity.SysDept;
import cn.datong.standard.entity.SysRole;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.entity.SysUserRole;
import cn.datong.standard.mapper.SysDeptMapper;
import cn.datong.standard.mapper.SysRoleMapper;
import cn.datong.standard.mapper.SysUserMapper;
import cn.datong.standard.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrgAssignmentService {
    static final String UNASSIGNABLE_MESSAGE = "该组织不能直接配置用户，请选择具体科室或车间";

    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;

    public boolean canAssignUsers(Long deptId) {
        List<SysDept> depts = depts();
        SysDept dept = depts.stream()
                .filter(item -> item.getId() != null && item.getId().equals(deptId))
                .findFirst()
                .orElse(null);
        return canAssignUsers(dept, depts);
    }

    public void requireAssignableDept(Long deptId) {
        if (!canAssignUsers(deptId)) {
            throw new BusinessException(403, UNASSIGNABLE_MESSAGE);
        }
    }

    public List<SysDept> assignableDepts() {
        List<SysDept> depts = depts();
        return depts.stream()
                .filter(dept -> canAssignUsers(dept, depts))
                .sorted(Comparator.comparing(SysDept::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    public DeptDetail detail(Long deptId) {
        SysDept dept = deptMapper.selectById(deptId);
        if (dept == null) {
            throw new BusinessException("组织不存在");
        }
        List<SysDept> depts = depts();
        boolean assignable = canAssignUsers(dept, depts);
        Map<Long, String> deptNames = deptNames(depts);
        Set<Long> adminUserIds = adminUserIds();
        List<UserView> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getDeptId, deptId)
                        .eq(SysUser::getDeleted, 0))
                .stream()
                .map(user -> toUserView(user, adminUserIds.contains(user.getId()), deptNames.get(user.getDeptId())))
                .toList();
        List<UserView> admins = users.stream()
                .filter(UserView::admin)
                .toList();
        return new DeptDetail(
                dept,
                assignable,
                assignable ? "该组织可配置用户和管理员" : UNASSIGNABLE_MESSAGE,
                users,
                admins
        );
    }

    public Set<Long> adminUserIds() {
        SysRole adminRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, UserAdminService.ADMIN_ROLE_CODE));
        if (adminRole == null) {
            return Set.of();
        }
        return userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getRoleId, adminRole.getId()))
                .stream()
                .map(SysUserRole::getUserId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    public Map<Long, String> deptNames() {
        return deptNames(depts());
    }

    private boolean canAssignUsers(SysDept dept, List<SysDept> depts) {
        if (dept == null || dept.getId() == null) {
            return false;
        }
        boolean topLevel = dept.getParentId() == null || dept.getParentId() == 0;
        boolean hasChild = depts.stream()
                .anyMatch(item -> dept.getId().equals(item.getParentId()));
        return !topLevel || !hasChild;
    }

    private List<SysDept> depts() {
        return deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeleted, 0));
    }

    private Map<Long, String> deptNames(List<SysDept> depts) {
        return depts.stream()
                .filter(dept -> dept.getId() != null)
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (left, right) -> left));
    }

    private UserView toUserView(SysUser user, boolean admin, String deptName) {
        if (Boolean.TRUE.equals(user.getIsSuperAdmin())) {
            return UserView.of(user, "超级管理员", true, deptName);
        }
        if (admin) {
            return UserView.of(user, "管理员", true, deptName);
        }
        return UserView.of(user, "普通用户", false, deptName);
    }
}
