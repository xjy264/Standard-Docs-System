package cn.datong.standard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_storage_stat")
public class SysStorageStat {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String statType;
    private Long targetId;
    private Long fileCount;
    private Long totalSize;
    private LocalDateTime updatedAt;
}
