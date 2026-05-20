package cn.datong.standard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_doc_submission")
public class SysDocSubmission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long itemId;
    private Long categoryId;
    private Long sectionDeptId;
    private Long workshopDeptId;
    private Long uploadUserId;
    private LocalDateTime submittedAt;

    @TableField(exist = false)
    private String sectionDeptName;
    @TableField(exist = false)
    private String categoryName;
    @TableField(exist = false)
    private String itemName;
    @TableField(exist = false)
    private String workshopDeptName;
    @TableField(exist = false)
    private String uploadUserName;
    @TableField(exist = false)
    private Integer attachmentCount;
    @TableField(exist = false)
    private List<SysDocAttachment> attachments;
}
