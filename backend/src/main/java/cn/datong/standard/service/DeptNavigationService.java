package cn.datong.standard.service;

import cn.datong.standard.dto.DeptNavigationItem;
import cn.datong.standard.entity.SysDept;
import cn.datong.standard.mapper.SysDeptMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeptNavigationService {
    private static final long SECTION_GROUP_ID = -1L;
    private static final long WORKSHOP_GROUP_ID = -2L;

    private final SysDeptMapper deptMapper;

    public List<DeptNavigationItem> navigation(Long userDeptId, boolean superAdmin) {
        List<SysDept> depts = depts();
        Set<Long> visibleIds = visibleDeptIds(userDeptId, superAdmin, depts);
        return buildNavigationGroups(depts, visibleIds);
    }

    public boolean canViewDept(Long userDeptId, boolean superAdmin, Long targetDeptId) {
        if (targetDeptId == null) {
            return false;
        }
        if (superAdmin) {
            return true;
        }
        return visibleDeptIds(userDeptId, false, depts()).contains(targetDeptId);
    }

    private Set<Long> visibleDeptIds(Long userDeptId, boolean superAdmin, List<SysDept> depts) {
        if (superAdmin) {
            return depts.stream()
                    .map(SysDept::getId)
                    .collect(Collectors.toCollection(HashSet::new));
        }
        if (userDeptId == null) {
            return Set.of();
        }
        Long agencyDeptId = agencyDeptId(depts);
        Long userTopDeptId = OrgScopeSupport.topLevelDeptId(userDeptId, depts);
        Set<Long> visibleIds = new HashSet<>();
        if (agencyDeptId != null) {
            visibleIds.addAll(OrgScopeSupport.descendantDeptIds(agencyDeptId, depts));
        }
        if (agencyDeptId != null && agencyDeptId.equals(userTopDeptId)) {
            for (SysDept topDept : OrgScopeSupport.topLevelDepts(depts)) {
                if (topDept.getId() != null && !topDept.getId().equals(agencyDeptId)) {
                    visibleIds.addAll(OrgScopeSupport.descendantDeptIds(topDept.getId(), depts));
                }
            }
        } else if (userTopDeptId != null) {
            visibleIds.addAll(OrgScopeSupport.descendantDeptIds(userTopDeptId, depts));
        }
        return visibleIds;
    }

    private List<DeptNavigationItem> buildNavigationGroups(List<SysDept> depts, Set<Long> visibleIds) {
        Long agencyDeptId = agencyDeptId(depts);
        List<DeptNavigationItem> result = new ArrayList<>();

        List<DeptNavigationItem> sectionItems = depts.stream()
                .filter(dept -> dept.getId() != null && visibleIds.contains(dept.getId()))
                .filter(dept -> agencyDeptId != null && agencyDeptId.equals(dept.getParentId()))
                .sorted(deptComparator())
                .map(this::toLeafItem)
                .toList();
        if (!sectionItems.isEmpty()) {
            result.add(groupItem(SECTION_GROUP_ID, "科室", sectionItems));
        }

        List<DeptNavigationItem> workshopItems = OrgScopeSupport.topLevelDepts(depts).stream()
                .filter(dept -> dept.getId() != null && visibleIds.contains(dept.getId()))
                .filter(dept -> agencyDeptId == null || !agencyDeptId.equals(dept.getId()))
                .sorted(deptComparator())
                .map(this::toLeafItem)
                .toList();
        if (!workshopItems.isEmpty()) {
            result.add(groupItem(WORKSHOP_GROUP_ID, "车间", workshopItems));
        }
        return result;
    }

    private DeptNavigationItem groupItem(Long id, String deptName, List<DeptNavigationItem> children) {
        return new DeptNavigationItem(
                id,
                0L,
                deptName,
                deptName,
                0,
                "ENABLED",
                children
        );
    }

    private DeptNavigationItem toLeafItem(SysDept dept) {
        return new DeptNavigationItem(
                dept.getId(),
                dept.getParentId(),
                dept.getDeptName(),
                dept.getDeptCode(),
                dept.getSortOrder(),
                dept.getStatus(),
                List.of()
        );
    }

    private Comparator<SysDept> deptComparator() {
        return Comparator
                .comparing(SysDept::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SysDept::getId, Comparator.nullsLast(Long::compareTo));
    }

    private Long agencyDeptId(List<SysDept> depts) {
        for (SysDept dept : OrgScopeSupport.topLevelDepts(depts)) {
            if ("机关".equals(dept.getDeptName())) {
                return dept.getId();
            }
        }
        return null;
    }

    private List<SysDept> depts() {
        List<SysDept> result = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeleted, 0)
                .ne(SysDept::getDeptType, FixedDocNavigation.DOC_SECTION)
                .orderByAsc(SysDept::getSortOrder));
        return result == null ? new ArrayList<>() : result.stream()
                .filter(dept -> !FixedDocNavigation.isDocSection(dept))
                .toList();
    }
}
