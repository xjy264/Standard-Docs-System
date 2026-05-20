package cn.datong.standard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_doc_submission_value")
public class SysDocSubmissionValue {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Long fieldId;
    private String fieldValue;

    @TableField(exist = false)
    private String fieldName;
    @TableField(exist = false)
    private String fieldType;
}
