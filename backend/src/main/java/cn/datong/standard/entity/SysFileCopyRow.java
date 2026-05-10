package cn.datong.standard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_file_copy")
public class SysFileCopyRow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fileId;
    private Long senderId;
    private String receiverType;
    private Long receiverId;
    private Long receiverUserId;
    private String readStatus;
    private LocalDateTime readTime;
    private LocalDateTime createdAt;
}
