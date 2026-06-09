package cn.datong.standard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_doc_item_attachment")
public class SysDocItemAttachment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long itemId;
    private String originalFileName;
    private String extension;
    private String mimeType;
    private Long fileSize;
    private String storageBucket;
    private String storagePath;
    private Long uploadedBy;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String uploadedByName;
}
