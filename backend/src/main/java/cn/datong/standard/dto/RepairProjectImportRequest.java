package cn.datong.standard.dto;

import java.util.List;

public record RepairProjectImportRequest(
        Long templateId,
        List<Long> templateItemIds,
        String projectFolderName,
        Integer docYear
) {
}
