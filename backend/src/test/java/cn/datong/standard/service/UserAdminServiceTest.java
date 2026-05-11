package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.dto.CurrentUser;
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
import java.util.Map;
import java.util.Set;

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

    @Test
    void listUserViewsFiltersByNamePhoneAndDeptForSuperAdmin() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        OrgAssignmentService orgAssignmentService = mock(OrgAssignmentService.class);
        SysUser target = user(2L, 25L, false);
        target.setRealName("张三");
        target.setPhone("13900000001");
        SysUser otherName = user(3L, 25L, false);
        otherName.setRealName("李四");
        otherName.setPhone("13900000002");
        SysUser otherPhone = user(4L, 25L, false);
        otherPhone.setRealName("张五");
        otherPhone.setPhone("13800000003");
        SysUser otherDept = user(5L, 26L, false);
        otherDept.setRealName("张六");
        otherDept.setPhone("13900000004");
        when(userMapper.selectList(any())).thenReturn(List.of(target, otherName, otherPhone, otherDept));
        when(orgAssignmentService.adminUserIds()).thenReturn(Set.of());
        when(orgAssignmentService.deptNames()).thenReturn(Map.of(25L, "计财科", 26L, "技术科"));
        UserAdminService service = new UserAdminService(
                userMapper,
                mock(SysUserRoleMapper.class),
                mock(SysRoleMapper.class),
                mock(SysDeptMapper.class),
                orgAssignmentService
        );

        List<cn.datong.standard.dto.UserView> result = service.listUserViews(
                25L,
                null,
                "张",
                "139",
                new CurrentUser(1L, 24L, true)
        );

        assertThat(result).extracting("id").containsExactly(2L);
    }

    @Test
    void listUserViewsKeepsAdminLimitedToOwnDeptWhenDeptFilterIsDifferent() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        OrgAssignmentService orgAssignmentService = mock(OrgAssignmentService.class);
        SysUser sameDept = user(2L, 25L, false);
        sameDept.setRealName("张三");
        sameDept.setPhone("13900000001");
        SysUser otherDept = user(3L, 26L, false);
        otherDept.setRealName("张四");
        otherDept.setPhone("13900000002");
        when(userMapper.selectList(any())).thenReturn(List.of(sameDept, otherDept));
        when(orgAssignmentService.adminUserIds()).thenReturn(Set.of(99L));
        when(orgAssignmentService.deptNames()).thenReturn(Map.of(25L, "计财科", 26L, "技术科"));
        UserAdminService service = new UserAdminService(
                userMapper,
                mock(SysUserRoleMapper.class),
                mock(SysRoleMapper.class),
                mock(SysDeptMapper.class),
                orgAssignmentService
        );

        List<cn.datong.standard.dto.UserView> result = service.listUserViews(
                26L,
                null,
                "张",
                "139",
                new CurrentUser(99L, 25L, false)
        );

        assertThat(result).extracting("id").containsExactly(2L);
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
