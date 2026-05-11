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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAdminServiceTest {

    @Test
    void superAdminCanPromoteNormalUserToAdmin() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysUser user = user(2L, 7L, false);
        SysRole adminRole = role(2L, "SEGMENT_ADMIN");
        when(userMapper.selectById(2L)).thenReturn(user);
        when(roleMapper.selectOne(any())).thenReturn(adminRole);
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        when(deptMapper.selectList(any())).thenReturn(List.of(dept(7L, 0L, "秦皇岛房建车间")));
        OrgAssignmentService orgAssignmentService = new OrgAssignmentService(deptMapper, userMapper, userRoleMapper, roleMapper);
        UserAdminService service = new UserAdminService(userMapper, userRoleMapper, roleMapper, deptMapper, orgAssignmentService);

        service.promoteAdmin(2L, true);

        verify(userRoleMapper).insert(any(SysUserRole.class));
    }

    @Test
    void nonSuperAdminCannotPromoteUser() {
        UserAdminService service = new UserAdminService(
                mock(SysUserMapper.class),
                mock(SysUserRoleMapper.class),
                mock(SysRoleMapper.class),
                mock(SysDeptMapper.class),
                mock(OrgAssignmentService.class)
        );

        assertThatThrownBy(() -> service.promoteAdmin(2L, false))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只有超级管理员可以设置管理员");
    }

    @Test
    void demoteAdminFallsBackToNormalUserRole() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        when(userMapper.selectById(2L)).thenReturn(user(2L, 7L, false));
        when(roleMapper.selectOne(any())).thenReturn(role(5L, "STAFF"));
        UserAdminService service = new UserAdminService(userMapper, userRoleMapper, roleMapper, mock(SysDeptMapper.class), mock(OrgAssignmentService.class));

        service.demoteAdmin(2L, true);

        verify(userRoleMapper).delete(any());
        verify(userRoleMapper).insert(any(SysUserRole.class));
    }

    @Test
    void adminCoverageReportsMissingTopLevelAdministrator() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysDept top = dept(7L, 0L, "秦皇岛房建车间");
        SysDept child = dept(25L, 7L, "下级班组");
        when(deptMapper.selectList(any())).thenReturn(List.of(top, child));
        when(roleMapper.selectOne(any())).thenReturn(role(2L, "SEGMENT_ADMIN"));
        when(userRoleMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.selectList(any())).thenReturn(List.of());
        OrgAssignmentService orgAssignmentService = new OrgAssignmentService(deptMapper, userMapper, userRoleMapper, roleMapper);
        UserAdminService service = new UserAdminService(userMapper, userRoleMapper, roleMapper, deptMapper, orgAssignmentService);

        List<DeptAdminCoverage> coverage = service.adminCoverage(true);

        assertThat(coverage).singleElement().satisfies(item -> {
            assertThat(item.deptId()).isEqualTo(25L);
            assertThat(item.missingAdmin()).isTrue();
            assertThat(item.adminCount()).isZero();
        });
    }

    @Test
    void adminCoverageCountsAdministratorInAssignableDept() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysDept top = dept(24L, 0L, "机关");
        SysDept child = dept(25L, 24L, "计财科");
        SysUser admin = user(19L, 25L, false);
        admin.setRealName("何悦");
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(19L);
        userRole.setRoleId(2L);
        when(deptMapper.selectList(any())).thenReturn(List.of(top, child));
        when(roleMapper.selectOne(any())).thenReturn(role(2L, "SEGMENT_ADMIN"));
        when(userRoleMapper.selectList(any())).thenReturn(List.of(userRole));
        when(userMapper.selectList(any())).thenReturn(List.of(admin));
        OrgAssignmentService orgAssignmentService = new OrgAssignmentService(deptMapper, userMapper, userRoleMapper, roleMapper);
        UserAdminService service = new UserAdminService(userMapper, userRoleMapper, roleMapper, deptMapper, orgAssignmentService);

        List<DeptAdminCoverage> coverage = service.adminCoverage(true);

        assertThat(coverage).singleElement().satisfies(item -> {
            assertThat(item.deptId()).isEqualTo(25L);
            assertThat(item.missingAdmin()).isFalse();
            assertThat(item.adminNames()).containsExactly("何悦");
        });
    }

    @Test
    void adminCoverageRequiresSectionsAndWorkshopsButNotAgency() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        when(deptMapper.selectList(any())).thenReturn(List.of(
                dept(24L, 0L, "机关"),
                dept(31L, 24L, "技术科"),
                dept(32L, 24L, "安全科"),
                dept(7L, 0L, "房建车间"),
                dept(8L, 0L, "公寓车间")
        ));
        when(roleMapper.selectOne(any())).thenReturn(role(2L, "SEGMENT_ADMIN"));
        when(userRoleMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.selectList(any())).thenReturn(List.of());
        OrgAssignmentService orgAssignmentService = new OrgAssignmentService(deptMapper, userMapper, userRoleMapper, roleMapper);
        UserAdminService service = new UserAdminService(userMapper, userRoleMapper, roleMapper, deptMapper, orgAssignmentService);

        List<DeptAdminCoverage> coverage = service.adminCoverage(true);

        assertThat(coverage).extracting(DeptAdminCoverage::deptId)
                .containsExactlyInAnyOrder(31L, 32L, 7L, 8L);
        assertThat(coverage).allSatisfy(item -> assertThat(item.missingAdmin()).isTrue());
    }

    @Test
    void cannotPromoteUserInUnassignableDept() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysUser user = user(2L, 24L, false);
        when(userMapper.selectById(2L)).thenReturn(user);
        when(deptMapper.selectList(any())).thenReturn(List.of(
                dept(24L, 0L, "机关"),
                dept(25L, 24L, "计财科")
        ));
        OrgAssignmentService orgAssignmentService = new OrgAssignmentService(deptMapper, userMapper, userRoleMapper, roleMapper);
        UserAdminService service = new UserAdminService(userMapper, userRoleMapper, roleMapper, deptMapper, orgAssignmentService);

        assertThatThrownBy(() -> service.promoteAdmin(2L, true))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该组织不能直接配置用户，请选择具体科室或车间");
    }

    private SysUser user(Long id, Long deptId, boolean superAdmin) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setDeptId(deptId);
        user.setIsSuperAdmin(superAdmin);
        user.setRealName("用户" + id);
        return user;
    }

    private SysRole role(Long id, String roleCode) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleCode(roleCode);
        return role;
    }

    private SysDept dept(Long id, Long parentId, String name) {
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setParentId(parentId);
        dept.setDeptName(name);
        return dept;
    }
}
