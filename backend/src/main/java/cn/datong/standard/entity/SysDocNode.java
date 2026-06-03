package cn.datong.standard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@TableName("sys_doc_node")
public class SysDocNode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sectionDeptId;
    private Long parentId;
    private String nodeType;
    private String nodeName;
    private Long itemId;
    private Integer sortOrder;
    private Integer level;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private Integer attachmentEnabled;
    @TableField(exist = false)
    private Integer submissionCount;
    @TableField(exist = false)
    private String fileType;
    private Integer docYear;
    @TableField(exist = false)
    private List<SysDocNode> children = new ArrayList<>();
}
