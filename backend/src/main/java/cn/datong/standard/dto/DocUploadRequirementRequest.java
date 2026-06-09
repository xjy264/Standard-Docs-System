package cn.datong.standard.dto;

public record DocUploadRequirementRequest(
        Long id,
        String requirementName,
        Integer sortOrder
) {
}
