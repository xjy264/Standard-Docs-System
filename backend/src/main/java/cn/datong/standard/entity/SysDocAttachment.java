package cn.datong.standard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_doc_attachment")
public class SysDocAttachment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Long requirementId;
    private String originalFileName;
    private String extension;
    private String mimeType;
    private Long fileSize;
    private String storageBucket;
    private String storagePath;
    private Long uploadedBy;
    private LocalDateTime createdAt;
    private Integer deleted;
    private LocalDateTime deletedAt;
    private Long deletedBy;

    @TableField(exist = false)
    private String uploadedByName;
    @TableField(exist = false)
    private String requirementName;
}
