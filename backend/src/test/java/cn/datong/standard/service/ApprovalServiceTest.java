package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.entity.SysDept;
import cn.datong.standard.entity.SysRegisterApproval;
import cn.datong.standard.entity.SysRole;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.entity.SysUserRole;
import cn.datong.standard.mapper.SysDeptMapper;
import cn.datong.standard.mapper.SysRegisterApprovalMapper;
import cn.datong.standard.mapper.SysRoleMapper;
import cn.datong.standard.mapper.SysUserMapper;
import cn.datong.standard.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalServiceTest {

    @Test
    void superAdminCanSeeAllPendingApprovals() {
        SysRegisterApprovalMapper approvalMapper = mock(SysRegisterApprovalMapper.class);
        when(approvalMapper.selectList(any())).thenReturn(List.of(approval(1L, 10L), approval(2L, 20L)));
        ApprovalService service = new ApprovalService(
                approvalMapper,
                mock(SysUserMapper.class),
                mock(SysDeptMapper.class),
                mock(SysUserRoleMapper.class),
                mock(SysRoleMapper.class)
        );

        List<SysRegisterApproval> result = service.pending(new CurrentUser(1L, 24L, true));

        assertThat(result).extracting(SysRegisterApproval::getUserId).containsExactly(10L, 20L);
    }

    @Test
    void adminOnlySeesPendingApprovalsInOwnDept() {
        SysRegisterApprovalMapper approvalMapper = mock(SysRegisterApprovalMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        when(approvalMapper.selectList(any())).thenReturn(List.of(approval(1L, 10L), approval(2L, 20L)));
        when(userMapper.selectById(19L)).thenReturn(user(19L, 25L));
        when(userMapper.selectById(10L)).thenReturn(user(10L, 25L));
        when(userMapper.selectById(20L)).thenReturn(user(20L, 7L));
        when(roleMapper.selectOne(any())).thenReturn(role(2L, "SEGMENT_ADMIN"));
        when(userRoleMapper.selectList(any())).thenReturn(List.of(userRole(19L, 2L)));
        ApprovalService service = new ApprovalService(approvalMapper, userMapper, deptMapper, userRoleMapper, roleMapper);

        List<SysRegisterApproval> result = service.pending(new CurrentUser(19L, 24L, false));

        assertThat(result).extracting(SysRegisterApproval::getUserId).containsExactly(10L);
    }

    @Test
    void adminCannotApproveUserOutsideOwnDept() {
        SysRegisterApprovalMapper approvalMapper = mock(SysRegisterApprovalMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        when(approvalMapper.selectById(2L)).thenReturn(approval(2L, 20L));
        when(userMapper.selectById(19L)).thenReturn(user(19L, 25L));
        when(userMapper.selectById(20L)).thenReturn(user(20L, 7L));
        when(roleMapper.selectOne(any())).thenReturn(role(2L, "SEGMENT_ADMIN"));
        when(userRoleMapper.selectList(any())).thenReturn(List.of(userRole(19L, 2L)));
        ApprovalService service = new ApprovalService(approvalMapper, userMapper, deptMapper, userRoleMapper, roleMapper);

        assertThatThrownBy(() -> service.approve(2L, new CurrentUser(19L, 24L, false)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只能审核本科室或本车间的注册申请");
    }

    @Test
    void normalUserCannotAccessApprovals() {
        SysRegisterApprovalMapper approvalMapper = mock(SysRegisterApprovalMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        when(userMapper.selectById(3L)).thenReturn(user(3L, 8L));
        when(roleMapper.selectOne(any())).thenReturn(role(2L, "SEGMENT_ADMIN"));
        ApprovalService service = new ApprovalService(
                approvalMapper,
                userMapper,
                mock(SysDeptMapper.class),
                mock(SysUserRoleMapper.class),
                roleMapper
        );

        assertThatThrownBy(() -> service.pending(new CurrentUser(3L, 8L, false)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("没有注册审核权限");
    }

    @Test
    void approveAssignsStaffRoleWhenRegisteredUserHasNoRole() {
        SysRegisterApprovalMapper approvalMapper = mock(SysRegisterApprovalMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysRegisterApproval approval = approval(1L, 10L);
        SysUser targetUser = user(10L, 25L);
        when(approvalMapper.selectById(1L)).thenReturn(approval);
        when(userMapper.selectById(10L)).thenReturn(targetUser);
        when(userRoleMapper.selectList(any())).thenReturn(List.of());
        when(roleMapper.selectOne(any())).thenReturn(role(5L, "STAFF"));
        ApprovalService service = new ApprovalService(
                approvalMapper,
                userMapper,
                mock(SysDeptMapper.class),
                userRoleMapper,
                roleMapper
        );

        service.approve(1L, new CurrentUser(1L, 24L, true));

        verify(userRoleMapper).insert(org.mockito.ArgumentMatchers.<SysUserRole>argThat(row ->
                row.getUserId().equals(10L) && row.getRoleId().equals(5L)
        ));
    }

    private SysRegisterApproval approval(Long id, Long userId) {
        SysRegisterApproval approval = new SysRegisterApproval();
        approval.setId(id);
        approval.setUserId(userId);
        approval.setApprovalStatus("PENDING");
        return approval;
    }

    private SysUser user(Long id, Long deptId) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setDeptId(deptId);
        user.setRealName("用户" + id);
        user.setIsSuperAdmin(false);
        return user;
    }

    private SysDept dept(Long id, Long parentId) {
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setParentId(parentId);
        return dept;
    }

    private SysRole role(Long id, String roleCode) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleCode(roleCode);
        return role;
    }

    private SysUserRole userRole(Long userId, Long roleId) {
        SysUserRole row = new SysUserRole();
        row.setUserId(userId);
        row.setRoleId(roleId);
        return row;
    }
}
