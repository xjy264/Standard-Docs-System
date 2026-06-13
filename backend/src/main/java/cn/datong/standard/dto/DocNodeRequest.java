package cn.datong.standard.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DocNodeRequest(
        Long sectionDeptId,
        Long parentId,
        String nodeName,
        Integer sortOrder,
        Long targetParentId,
        Boolean attachmentEnabled,
        String contentHtml,
        String fileType,
        Integer docYear,
        String businessType,
        String submitterMode,
        List<DocUploadRequirementRequest> requirements,
        LocalDateTime uploadDeadline,
        Boolean workshopUploadEnabled,
        List<Long> visibleWorkshopIds
) {
    public DocNodeRequest(Long sectionDeptId,
                          Long parentId,
                          String nodeName,
                          Integer sortOrder,
                          Long targetParentId,
                          Boolean attachmentEnabled,
                          String contentHtml,
                          String fileType) {
        this(sectionDeptId, parentId, nodeName, sortOrder, targetParentId, attachmentEnabled, contentHtml, fileType, null, null, null, null, null, null, null);
    }

    public DocNodeRequest(Long sectionDeptId,
                          Long parentId,
                          String nodeName,
                          Integer sortOrder,
                          Long targetParentId,
                          Boolean attachmentEnabled,
                          String contentHtml,
                          String fileType,
                          Integer docYear) {
        this(sectionDeptId, parentId, nodeName, sortOrder, targetParentId, attachmentEnabled, contentHtml, fileType, docYear, null, null, null, null, null, null);
    }

    public DocNodeRequest(Long sectionDeptId,
                          Long parentId,
                          String nodeName,
                          Integer sortOrder,
                          Long targetParentId,
                          Boolean attachmentEnabled,
                          String contentHtml,
                          String fileType,
                          Integer docYear,
                          String businessType,
                          String submitterMode,
                          List<DocUploadRequirementRequest> requirements) {
        this(sectionDeptId, parentId, nodeName, sortOrder, targetParentId, attachmentEnabled, contentHtml, fileType,
                docYear, businessType, submitterMode, requirements, null, null, null);
    }
}
