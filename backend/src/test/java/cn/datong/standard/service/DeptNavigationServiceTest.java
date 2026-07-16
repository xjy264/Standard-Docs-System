package cn.datong.standard.service;

import cn.datong.standard.dto.DeptNavigationItem;
import cn.datong.standard.entity.SysDept;
import cn.datong.standard.mapper.SysDeptMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeptNavigationServiceTest {

    @Test
    void sectionUserSeesAgencySectionsAndAllWorkshops() {
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        when(deptMapper.selectList(any())).thenReturn(defaultDepts());
        DeptNavigationService service = new DeptNavigationService(deptMapper);

        List<DeptNavigationItem> result = service.navigation(25L, false);

        assertThat(result).extracting(DeptNavigationItem::deptName)
                .containsExactly("科室", "车间");
        assertThat(result.get(0).children()).extracting(DeptNavigationItem::deptName)
                .containsExactly("计财科", "技术科");
        assertThat(result.get(1).children()).extracting(DeptNavigationItem::deptName)
                .containsExactly("房建车间", "公寓车间");
    }

    @Test
    void workshopUserSeesAgencySectionsAndOwnWorkshopOnly() {
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        when(deptMapper.selectList(any())).thenReturn(defaultDepts());
        DeptNavigationService service = new DeptNavigationService(deptMapper);

        List<DeptNavigationItem> result = service.navigation(7L, false);

        assertThat(result).extracting(DeptNavigationItem::deptName)
                .containsExactly("科室", "车间");
        assertThat(result.get(0).children()).extracting(DeptNavigationItem::deptName)
                .containsExactly("计财科", "技术科");
        assertThat(result.get(1).children()).extracting(DeptNavigationItem::deptName)
                .containsExactly("房建车间");
    }

    @Test
    void superAdminSeesAllSectionsAndWorkshopsEvenWithoutDepartment() {
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        when(deptMapper.selectList(any())).thenReturn(defaultDepts());
        DeptNavigationService service = new DeptNavigationService(deptMapper);

        List<DeptNavigationItem> result = service.navigation(null, true);

        assertThat(result).extracting(DeptNavigationItem::deptName)
                .containsExactly("科室", "车间");
        assertThat(result.get(0).children()).extracting(DeptNavigationItem::deptName)
                .containsExactly("计财科", "技术科");
        assertThat(result.get(1).children()).extracting(DeptNavigationItem::deptName)
                .containsExactly("房建车间", "公寓车间");
    }

    @Test
    void canViewDeptFollowsNavigationScope() {
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        when(deptMapper.selectList(any())).thenReturn(defaultDepts());
        DeptNavigationService service = new DeptNavigationService(deptMapper);

        assertThat(service.canViewDept(7L, false, 25L)).isTrue();
        assertThat(service.canViewDept(7L, false, 7L)).isTrue();
        assertThat(service.canViewDept(7L, false, 8L)).isFalse();
        assertThat(service.canViewDept(7L, true, 8L)).isTrue();
    }

    private List<SysDept> defaultDepts() {
        SysDept docSection = dept(99L, 24L, "技术规章", 5);
        docSection.setDeptType("DOC_SECTION");
        return List.of(
                dept(24L, 0L, "机关", 1),
                docSection,
                dept(25L, 24L, "计财科", 10),
                dept(26L, 24L, "技术科", 20),
                dept(7L, 0L, "房建车间", 30),
                dept(8L, 0L, "公寓车间", 40)
        );
    }

    private SysDept dept(Long id, Long parentId, String name, int sortOrder) {
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setParentId(parentId);
        dept.setDeptName(name);
        dept.setDeptCode("D" + id);
        dept.setSortOrder(sortOrder);
        dept.setStatus("ENABLED");
        return dept;
    }
}
