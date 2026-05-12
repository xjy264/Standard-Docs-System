package cn.datong.standard.dto;

import java.time.LocalDateTime;

public record ApprovalView(
        Long id,
        String userRealName,
        String phone,
        String deptName,
        String approvalStatus,
        LocalDateTime createdAt,
        LocalDateTime approvedAt,
        String approverName,
        String rejectReason
) {
}
