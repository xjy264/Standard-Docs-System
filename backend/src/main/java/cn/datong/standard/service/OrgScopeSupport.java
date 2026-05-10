package cn.datong.standard.service;

import cn.datong.standard.entity.SysDept;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class OrgScopeSupport {
    private OrgScopeSupport() {
    }

    static List<SysDept> topLevelDepts(List<SysDept> depts) {
        Map<Long, SysDept> deptMap = deptMap(depts);
        return depts.stream()
                .filter(dept -> isTopLevel(dept, deptMap))
                .toList();
    }

    static Set<Long> descendantDeptIds(Long topDeptId, List<SysDept> depts) {
        Set<Long> result = new HashSet<>();
        result.add(topDeptId);
        boolean changed;
        do {
            changed = false;
            for (SysDept dept : depts) {
                if (dept.getId() == null || result.contains(dept.getId())) {
                    continue;
                }
                if (dept.getParentId() != null && result.contains(dept.getParentId())) {
                    result.add(dept.getId());
                    changed = true;
                }
            }
        } while (changed);
        return result;
    }

    static Long topLevelDeptId(Long deptId, List<SysDept> depts) {
        if (deptId == null) {
            return null;
        }
        Map<Long, SysDept> deptMap = deptMap(depts);
        SysDept current = deptMap.get(deptId);
        if (current == null) {
            return deptId;
        }
        Set<Long> visited = new HashSet<>();
        while (current.getParentId() != null && current.getParentId() != 0 && deptMap.containsKey(current.getParentId())) {
            if (!visited.add(current.getId())) {
                break;
            }
            current = deptMap.get(current.getParentId());
        }
        return current.getId();
    }

    private static boolean isTopLevel(SysDept dept, Map<Long, SysDept> deptMap) {
        Long parentId = dept.getParentId();
        return parentId == null || parentId == 0 || !deptMap.containsKey(parentId);
    }

    private static Map<Long, SysDept> deptMap(List<SysDept> depts) {
        Map<Long, SysDept> result = new HashMap<>();
        for (SysDept dept : depts) {
            if (dept.getId() != null) {
                result.put(dept.getId(), dept);
            }
        }
        return result;
    }
}
