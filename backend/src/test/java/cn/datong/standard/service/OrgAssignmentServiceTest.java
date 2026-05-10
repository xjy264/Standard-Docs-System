package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.entity.SysDept;
import cn.datong.standard.entity.SysRole;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.entity.SysUserRole;
import cn.datong.standard.mapper.SysDeptMapper;
import cn.datong.standard.mapper.SysRoleMapper;
import cn.datong.standard.mapper.SysUserMapper;
import cn.datong.standard.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrgAssignmentServiceTest {

    @Test
    void agencyWithChildDepartmentsCannotAssignUsers() {
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        when(deptMapper.selectList(any())).thenReturn(List.of(
                dept(24L, 0L, "机关"),
                dept(25L, 24L, "计财科")
        ));
        OrgAssignmentService service = new OrgAssignmentService(
                deptMapper,
                mock(SysUserMapper.class),
                mock(SysUserRoleMapper.class),
                mock(SysRoleMapper.class)
        );

        assertThat(service.canAssignUsers(24L)).isFalse();
        assertThatThrownBy(() -> service.requireAssignableDept(24L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该组织不能直接配置用户，请选择具体科室或车间");
    }

    @Test
    void sectionAndWorkshopCanAssignUsers() {
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        when(deptMapper.selectList(any())).thenReturn(List.of(
                dept(24L, 0L, "机关"),
                dept(25L, 24L, "计财科"),
                dept(7L, 0L, "秦皇岛房建车间")
        ));
        OrgAssignmentService service = new OrgAssignmentService(
                deptMapper,
                mock(SysUserMapper.class),
                mock(SysUserRoleMapper.class),
                mock(SysRoleMapper.class)
        );

        assertThat(service.canAssignUsers(25L)).isTrue();
        assertThat(service.canAssignUsers(7L)).isTrue();
    }

    @Test
    void detailReturnsDirectUsersAndDirectAdministrators() {
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysUser admin = user(19L, 25L, "何悦");
        SysUser normal = user(20L, 25L, "张明");
        when(deptMapper.selectList(any())).thenReturn(List.of(
                dept(24L, 0L, "机关"),
                dept(25L, 24L, "计财科")
        ));
        when(deptMapper.selectById(25L)).thenReturn(dept(25L, 24L, "计财科"));
        when(userMapper.selectList(any())).thenReturn(List.of(admin, normal));
        when(roleMapper.selectOne(any())).thenReturn(role(2L, "SEGMENT_ADMIN"));
        when(userRoleMapper.selectList(any())).thenReturn(List.of(userRole(19L, 2L)));
        OrgAssignmentService service = new OrgAssignmentService(deptMapper, userMapper, userRoleMapper, roleMapper);

        DeptDetail detail = service.detail(25L);

        assertThat(detail.assignable()).isTrue();
        assertThat(detail.users()).extracting("realName").containsExactly("何悦", "张明");
        assertThat(detail.admins()).extracting("realName").containsExactly("何悦");
    }

    private SysDept dept(Long id, Long parentId, String name) {
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setParentId(parentId);
        dept.setDeptName(name);
        return dept;
    }

    private SysUser user(Long id, Long deptId, String realName) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setDeptId(deptId);
        user.setRealName(realName);
        user.setUsername("u" + id);
        user.setIsSuperAdmin(false);
        return user;
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
