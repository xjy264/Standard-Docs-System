package cn.datong.standard.dto;

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
        String businessType,
        String submitterMode,
        List<DocUploadRequirementRequest> requirements
) {
    public DocNodeRequest(Long sectionDeptId,
                          Long parentId,
                          String nodeName,
                          Integer sortOrder,
                          Long targetParentId,
                          Boolean attachmentEnabled,
                          String contentHtml,
                          String fileType) {
        this(sectionDeptId, parentId, nodeName, sortOrder, targetParentId, attachmentEnabled, contentHtml, fileType, null, null, null);
    }
}
