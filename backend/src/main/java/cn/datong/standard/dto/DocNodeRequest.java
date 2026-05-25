package cn.datong.standard.dto;

public record DocNodeRequest(
        Long sectionDeptId,
        Long parentId,
        String nodeName,
        Integer sortOrder,
        Long targetParentId,
        Boolean attachmentEnabled,
        String contentHtml
) {
}
