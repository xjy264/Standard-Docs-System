package cn.datong.standard.service;

import java.util.List;

public record DeptAdminCoverage(
        Long deptId,
        String deptName,
        int adminCount,
        List<String> adminNames,
        boolean missingAdmin
) {
}
