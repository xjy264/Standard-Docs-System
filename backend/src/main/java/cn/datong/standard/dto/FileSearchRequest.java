package cn.datong.standard.dto;

import cn.datong.standard.enums.VisibilityScope;

import java.time.LocalDate;

public record FileSearchRequest(
        String keyword,
        String extension,
        LocalDate uploadStart,
        LocalDate uploadEnd,
        String ownerDeptName,
        String ownerName,
        Long deptId,
        Long folderId,
        VisibilityScope visibilityScope,
        Boolean mine
) {
}
