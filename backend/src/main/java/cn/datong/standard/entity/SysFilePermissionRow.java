package cn.datong.standard.entity;

import cn.datong.standard.enums.TargetType;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_file_permission")
public class SysFilePermissionRow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fileId;
    @EnumValue
    private TargetType targetType;
    private Long targetId;
    private String accessType;
    private Long createdBy;
    private LocalDateTime createdAt;
}
