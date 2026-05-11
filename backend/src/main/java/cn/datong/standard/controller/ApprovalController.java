package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.entity.SysRegisterApproval;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.security.SecurityUtils;
import cn.datong.standard.service.OperationLogService;
import cn.datong.standard.service.ApprovalService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {
    private final ApprovalService approvalService;
    private final OperationLogService logService;

    @GetMapping("/pending")
    public ApiResponse<List<SysRegisterApproval>> pending() {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(approvalService.pending(currentUser));
    }

    @GetMapping("/history")
    public ApiResponse<List<SysRegisterApproval>> history() {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(approvalService.history(currentUser));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable Long id, HttpServletRequest request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        SysUser user = approvalService.approve(id, currentUser);
        logService.operation(currentUser.userId(), "注册审批通过", "USER", user.getId(), "SUCCESS", null, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id, @RequestParam(required = false) String reason,
                                    HttpServletRequest request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        SysUser user = approvalService.reject(id, reason, currentUser);
        logService.operation(currentUser.userId(), "注册审批拒绝", "USER", user.getId(), "SUCCESS", null, request);
        return ApiResponse.success();
    }
}
