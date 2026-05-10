package cn.datong.standard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_recycle_bin")
public class SysRecycleBin {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fileId;
    private String originalPath;
    private Long deletedBy;
    private LocalDateTime deletedAt;
    private Long deptId;
    private Long fileSize;
    private String status;
}
