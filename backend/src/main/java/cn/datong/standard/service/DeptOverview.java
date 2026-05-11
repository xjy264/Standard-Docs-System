package cn.datong.standard.service;

import java.util.List;

public record DeptOverview(
        Long id,
        Long parentId,
        String deptName,
        String deptCode,
        Integer sortOrder,
        String status,
        long userCount,
        long fileCount,
        List<String> adminNames,
        boolean missingAdmin,
        boolean adminRequired
) {
}
