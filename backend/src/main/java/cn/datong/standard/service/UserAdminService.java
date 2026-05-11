package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.dto.CurrentUser;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAdminService {
    static final String ADMIN_ROLE_CODE = "SEGMENT_ADMIN";
    static final String STAFF_ROLE_CODE = "STAFF";

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;
    private final OrgAssignmentService orgAssignmentService;

    public void promoteAdmin(Long userId, boolean currentUserSuperAdmin) {
        requireSuperAdmin(currentUserSuperAdmin, "只有超级管理员可以设置管理员");
        SysUser user = requireUser(userId);
        if (Boolean.TRUE.equals(user.getIsSuperAdmin())) {
            throw new BusinessException("超级管理员无需设置为管理员");
        }
        orgAssignmentService.requireAssignableDept(user.getDeptId());
        replaceRole(userId, requireRole(ADMIN_ROLE_CODE));
    }

    public void demoteAdmin(Long userId, boolean currentUserSuperAdmin) {
        requireSuperAdmin(currentUserSuperAdmin, "只有超级管理员可以取消管理员");
        SysUser user = requireUser(userId);
        if (Boolean.TRUE.equals(user.getIsSuperAdmin())) {
            throw new BusinessException("不能取消超级管理员身份");
        }
        replaceRole(userId, requireRole(STAFF_ROLE_CODE));
    }

    public List<DeptAdminCoverage> adminCoverage(boolean currentUserSuperAdmin) {
        requireSuperAdmin(currentUserSuperAdmin, "只有超级管理员可以查看管理员覆盖情况");
        List<SysDept> assignableDepts = orgAssignmentService.assignableDepts().stream()
                .sorted(Comparator.comparing(SysDept::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        Set<Long> adminUserIds = orgAssignmentService.adminUserIds();
        Map<Long, SysUser> userMap = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getDeleted, 0))
                .stream()
                .collect(Collectors.toMap(SysUser::getId, user -> user, (left, right) -> left));

        return assignableDepts.stream()
                .map(dept -> {
                    List<String> adminNames = adminUserIds.stream()
                            .map(userMap::get)
                            .filter(user -> user != null && dept.getId().equals(user.getDeptId()))
                            .map(this::displayName)
                            .sorted()
                            .toList();
                    return new DeptAdminCoverage(
                            dept.getId(),
                            dept.getDeptName(),
                            adminNames.size(),
                            adminNames,
                            adminNames.isEmpty()
                    );
                })
                .toList();
    }

    public List<UserView> listUserViews(Long deptId, String keyword) {
        return listUserViews(deptId, keyword, null);
    }

    public List<UserView> listUserViews(Long deptId, String keyword, CurrentUser currentUser) {
        return listUserViews(deptId, keyword, null, null, currentUser);
    }

    public List<UserView> listUserViews(Long deptId, String keyword, String realName, String phone, CurrentUser currentUser) {
        Long effectiveDeptId = deptId;
        if (currentUser != null && !currentUser.superAdmin()) {
            requireAdminUser(currentUser.userId());
            effectiveDeptId = currentUser.deptId();
        }
        Long filterDeptId = effectiveDeptId;
        String nameFilter = hasText(realName) ? realName : keyword;
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(filterDeptId != null, SysUser::getDeptId, filterDeptId)
                .like(hasText(nameFilter), SysUser::getRealName, nameFilter)
                .like(hasText(phone), SysUser::getPhone, phone));
        Set<Long> adminUserIds = orgAssignmentService.adminUserIds();
        Map<Long, String> deptNames = orgAssignmentService.deptNames();
        return users.stream()
                .filter(user -> filterDeptId == null || filterDeptId.equals(user.getDeptId()))
                .filter(user -> !hasText(nameFilter) || contains(user.getRealName(), nameFilter))
                .filter(user -> !hasText(phone) || contains(user.getPhone(), phone))
                .map(user -> toUserView(user, adminUserIds.contains(user.getId()), deptNames.get(user.getDeptId())))
                .toList();
    }

    public UserView userView(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toUserView(user, orgAssignmentService.adminUserIds().contains(userId),
                orgAssignmentService.deptNames().get(user.getDeptId()));
    }

    public UserView userView(Long userId, CurrentUser currentUser) {
        SysUser user = requireUser(userId);
        if (currentUser != null && !currentUser.superAdmin()) {
            requireAdminUser(currentUser.userId());
            if (currentUser.deptId() == null || !currentUser.deptId().equals(user.getDeptId())) {
                throw new BusinessException(403, "只能管理本科室或本车间的用户");
            }
        }
        return toUserView(user, orgAssignmentService.adminUserIds().contains(userId),
                orgAssignmentService.deptNames().get(user.getDeptId()));
    }

    public boolean isAdmin(Long userId) {
        return orgAssignmentService.adminUserIds().contains(userId);
    }

    public void requireAdminUser(Long userId) {
        if (!isAdmin(userId)) {
            throw new BusinessException(403, "没有用户管理权限");
        }
    }

    public void requireSameDeptManage(CurrentUser currentUser, Long targetUserId) {
        if (currentUser.superAdmin()) {
            return;
        }
        requireAdminUser(currentUser.userId());
        SysUser targetUser = requireUser(targetUserId);
        if (currentUser.deptId() == null || !currentUser.deptId().equals(targetUser.getDeptId())) {
            throw new BusinessException(403, "只能管理本科室或本车间的用户");
        }
    }

    public void requireCreateInManagedDept(CurrentUser currentUser, Long deptId) {
        orgAssignmentService.requireAssignableDept(deptId);
        if (currentUser.superAdmin()) {
            return;
        }
        requireAdminUser(currentUser.userId());
        if (currentUser.deptId() == null || !currentUser.deptId().equals(deptId)) {
            throw new BusinessException(403, "只能管理本科室或本车间的用户");
        }
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

    private SysUser requireUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private void replaceRole(Long userId, SysRole role) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        SysUserRole row = new SysUserRole();
        row.setUserId(userId);
        row.setRoleId(role.getId());
        userRoleMapper.insert(row);
    }

    private SysRole requireRole(String roleCode) {
        SysRole role = role(roleCode);
        if (role == null) {
            throw new BusinessException("角色不存在：" + roleCode);
        }
        return role;
    }

    private SysRole role(String roleCode) {
        return roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode));
    }

    private void requireSuperAdmin(boolean currentUserSuperAdmin, String message) {
        if (!currentUserSuperAdmin) {
            throw new BusinessException(403, message);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private String displayName(SysUser user) {
        if (user.getRealName() != null && !user.getRealName().isBlank()) {
            return user.getRealName();
        }
        return user.getUsername();
    }
}
