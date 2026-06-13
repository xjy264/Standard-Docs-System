package cn.datong.standard.dto;

public record RepairProjectImportRequest(
        Long templateId,
        String projectFolderName,
        Integer docYear
) {
}
