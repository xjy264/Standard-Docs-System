package cn.datong.standard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_register_approval")
public class SysRegisterApproval {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String approvalStatus;
    private Long approverId;
    private String rejectReason;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
}
