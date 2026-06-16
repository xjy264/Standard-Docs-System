package cn.datong.standard.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocRecycleBinItem {
    private Long id;
    private Long itemId;
    private String nodeName;
    private String fileType;
    private Integer docYear;
    private LocalDateTime deletedAt;
    private String deletedByName;
    private String originalParentName;
    private Long submissionCount;
    private Long attachmentCount;
}

