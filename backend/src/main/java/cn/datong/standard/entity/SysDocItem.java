package cn.datong.standard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_doc_item")
public class SysDocItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private String itemName;
    private String contentHtml;
    private Integer collectEnabled;
    private Integer attachmentEnabled;
    private Integer attachmentRequired;
    private Integer sortOrder;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private Integer fieldCount;
    @TableField(exist = false)
    private Integer submissionCount;
    @TableField(exist = false)
    private String categoryName;
    @TableField(exist = false)
    private Long sectionDeptId;
    @TableField(exist = false)
    private String sectionDeptName;
}
