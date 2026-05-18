package cn.datong.standard.dto;

import java.util.List;

public record DeptNavigationItem(
        Long id,
        Long parentId,
        String deptName,
        String deptCode,
        Integer sortOrder,
        String status,
        List<DeptNavigationItem> children
) {
}
