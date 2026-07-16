package cn.datong.standard.service;

import cn.datong.standard.entity.SysDept;
import cn.datong.standard.entity.SysDocCategory;
import cn.datong.standard.entity.SysDocItem;
import cn.datong.standard.entity.SysRole;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.entity.SysUserRole;
import cn.datong.standard.mapper.SysDeptMapper;
import cn.datong.standard.mapper.SysDocCategoryMapper;
import cn.datong.standard.mapper.SysDocItemMapper;
import cn.datong.standard.mapper.SysRoleMapper;
import cn.datong.standard.mapper.SysUserMapper;
import cn.datong.standard.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeptOverviewServiceTest {

    @Test
    void overviewCountsDirectUsersAndFilesAndAdminStatus() {
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysDocCategoryMapper categoryMapper = mock(SysDocCategoryMapper.class);
        SysDocItemMapper itemMapper = mock(SysDocItemMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        OrgAssignmentService orgAssignmentService = new OrgAssignmentService(deptMapper, userMapper, userRoleMapper, roleMapper);
        SysUser admin = user(10L, 25L, "何悦");
        SysUser sectionUser = user(11L, 25L, "张三");
        SysUser childUser = user(12L, 26L, "李四");
        SysUserRole adminRole = new SysUserRole();
        adminRole.setUserId(10L);
        adminRole.setRoleId(2L);
        when(deptMapper.selectList(any())).thenReturn(List.of(
                dept(24L, 0L, "机关"),
                dept(25L, 24L, "计划财务科", "SECTION", "FINANCE"),
                dept(26L, 25L, "下级组织"),
                dept(7L, 0L, "秦皇岛房建车间"),
                dept(99L, 24L, "技术规章", "DOC_SECTION", "DOC_TECH_RULES")
        ));
        when(userMapper.selectList(any())).thenReturn(List.of(admin, sectionUser, childUser));
        when(categoryMapper.selectList(any())).thenReturn(List.of(
                category(100L, 25L),
                category(101L, 26L),
                category(102L, 7L)
        ));
        when(itemMapper.selectList(any())).thenReturn(List.of(
                item(1L, 100L),
                item(2L, 100L),
                item(3L, 101L),
                item(4L, 102L)
        ));
        when(roleMapper.selectOne(any())).thenReturn(role(2L, "SEGMENT_ADMIN"));
        when(userRoleMapper.selectList(any())).thenReturn(List.of(adminRole));
        DeptOverviewService service = new DeptOverviewService(deptMapper, userMapper, categoryMapper, itemMapper, orgAssignmentService);

        List<DeptOverview> result = service.overview(true);

        assertThat(result).filteredOn(item -> item.id().equals(24L)).singleElement()
                .satisfies(item -> {
                    assertThat(item.adminRequired()).isFalse();
                    assertThat(item.missingAdmin()).isFalse();
                    assertThat(item.adminNames()).isEmpty();
                    assertThat(item.userCount()).isZero();
                    assertThat(item.fileCount()).isZero();
                });
        assertThat(result).filteredOn(item -> item.id().equals(25L)).singleElement()
                .satisfies(item -> {
                    assertThat(item.userCount()).isEqualTo(2);
                    assertThat(item.fileCount()).isEqualTo(2);
                    assertThat(item.adminRequired()).isTrue();
                    assertThat(item.missingAdmin()).isFalse();
                    assertThat(item.adminNames()).containsExactly("何悦");
                });
        assertThat(result).filteredOn(item -> item.id().equals(26L)).singleElement()
                .satisfies(item -> {
                    assertThat(item.userCount()).isEqualTo(1);
                    assertThat(item.fileCount()).isEqualTo(1);
                    assertThat(item.adminRequired()).isTrue();
                    assertThat(item.missingAdmin()).isTrue();
                });
        assertThat(result).noneMatch(item -> item.id().equals(99L));
        assertThat(result).filteredOn(item -> item.id().equals(25L)).singleElement()
                .satisfies(item -> assertThat(item.fixedNavigation()).isTrue());
    }

    private SysDept dept(Long id, Long parentId, String name) {
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setParentId(parentId);
        dept.setDeptName(name);
        dept.setSortOrder(id.intValue());
        dept.setStatus("ENABLED");
        return dept;
    }

    private SysDept dept(Long id, Long parentId, String name, String type, String code) {
        SysDept dept = dept(id, parentId, name);
        dept.setDeptType(type);
        dept.setDeptCode(code);
        return dept;
    }

    private SysUser user(Long id, Long deptId, String realName) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setDeptId(deptId);
        user.setRealName(realName);
        user.setUsername("u" + id);
        return user;
    }

    private SysDocCategory category(Long id, Long sectionDeptId) {
        SysDocCategory category = new SysDocCategory();
        category.setId(id);
        category.setSectionDeptId(sectionDeptId);
        return category;
    }

    private SysDocItem item(Long id, Long categoryId) {
        SysDocItem item = new SysDocItem();
        item.setId(id);
        item.setCategoryId(categoryId);
        return item;
    }

    private SysRole role(Long id, String roleCode) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleCode(roleCode);
        return role;
    }
}
