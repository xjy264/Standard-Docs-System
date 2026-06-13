package cn.datong.standard.dto;

import java.util.List;

public record SectionFileTreeItem(
        Long id,
        Long parentId,
        String deptName,
        String deptType,
        Long fileCount,
        List<SectionFileTreeItem> children
) {
}
