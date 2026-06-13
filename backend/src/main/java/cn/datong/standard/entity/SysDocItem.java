package cn.datong.standard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_doc_item")
public class SysDocItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private Long sectionDeptId;
    private String itemName;
    private String businessType;
    private String submitterMode;
    private String fileType;
    private Integer docYear;
    private String contentHtml;
    private Integer attachmentEnabled;
    private Integer sortOrder;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private Integer submissionCount;
    @TableField(exist = false)
    private String categoryName;
    @TableField(exist = false)
    private String sectionDeptName;
    @TableField(exist = false)
    private List<SysDocUploadRequirement> requirements;
    @TableField(exist = false)
    private List<SysDocItemAttachment> issuedAttachments;
}
