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
        List<Long> visibleWorkshopIds,
        Boolean showUploadProgress,
        String moduleType
) {
    public DocNodeRequest(Long sectionDeptId,
                          Long parentId,
                          String nodeName,
                          Integer sortOrder,
                          Long targetParentId,
                          Boolean attachmentEnabled,
                          String contentHtml,
                          String fileType,
                          String businessType,
                          String submitterMode,
                          List<DocUploadRequirementRequest> requirements) {
        this(sectionDeptId, parentId, nodeName, sortOrder, targetParentId, attachmentEnabled,
                contentHtml, fileType, null, businessType, submitterMode, requirements, null, null, null, null, null);
    }

    public DocNodeRequest(Long sectionDeptId,
                          Long parentId,
                          String nodeName,
                          Integer sortOrder,
                          Long targetParentId,
                          Boolean attachmentEnabled,
                          String contentHtml,
                          String fileType) {
        this(sectionDeptId, parentId, nodeName, sortOrder, targetParentId, attachmentEnabled,
                contentHtml, fileType, null, null, null, null, null, null, null, null, null);
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
        this(sectionDeptId, parentId, nodeName, sortOrder, targetParentId, attachmentEnabled,
                contentHtml, fileType, docYear, null, null, null, null, null, null, null, null);
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
                docYear, businessType, submitterMode, requirements, null, null, null, null, null);
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
                          Boolean showUploadProgress) {
        this(sectionDeptId, parentId, nodeName, sortOrder, targetParentId, attachmentEnabled, contentHtml, fileType,
                docYear, businessType, submitterMode, null, null, null, null, showUploadProgress, null);
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
                          List<DocUploadRequirementRequest> requirements,
                          LocalDateTime uploadDeadline,
                          Boolean workshopUploadEnabled,
                          List<Long> visibleWorkshopIds) {
        this(sectionDeptId, parentId, nodeName, sortOrder, targetParentId, attachmentEnabled, contentHtml, fileType,
                docYear, businessType, submitterMode, requirements, uploadDeadline, workshopUploadEnabled, visibleWorkshopIds, null, null);
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
                          List<DocUploadRequirementRequest> requirements,
                          LocalDateTime uploadDeadline,
                          Boolean workshopUploadEnabled,
                          List<Long> visibleWorkshopIds,
                          Boolean showUploadProgress,
                          String moduleType) {
        this.sectionDeptId = sectionDeptId;
        this.parentId = parentId;
        this.nodeName = nodeName;
        this.sortOrder = sortOrder;
        this.targetParentId = targetParentId;
        this.attachmentEnabled = attachmentEnabled;
        this.contentHtml = contentHtml;
        this.fileType = fileType;
        this.docYear = docYear;
        this.businessType = businessType;
        this.submitterMode = submitterMode;
        this.requirements = requirements;
        this.uploadDeadline = uploadDeadline;
        this.workshopUploadEnabled = workshopUploadEnabled;
        this.visibleWorkshopIds = visibleWorkshopIds;
        this.showUploadProgress = showUploadProgress;
        this.moduleType = moduleType;
    }
}
