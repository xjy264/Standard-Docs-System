package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.dto.ApprovalView;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.entity.SysDept;
import cn.datong.standard.entity.SysRegisterApproval;
import cn.datong.standard.entity.SysRole;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.entity.SysUserRole;
import cn.datong.standard.mapper.SysDeptMapper;
import cn.datong.standard.mapper.SysRegisterApprovalMapper;
import cn.datong.standard.mapper.SysRoleMapper;
import cn.datong.standard.mapper.SysUserMapper;
import cn.datong.standard.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApprovalService {
    private final SysRegisterApprovalMapper approvalMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;

    public List<ApprovalView> pending(CurrentUser currentUser) {
        List<SysRegisterApproval> pending = approvalMapper.selectList(new LambdaQueryWrapper<SysRegisterApproval>()
                .eq(SysRegisterApproval::getApprovalStatus, "PENDING")
                .orderByDesc(SysRegisterApproval::getCreatedAt));
        pending = pending.stream()
                .filter(approval -> "PENDING".equals(approval.getApprovalStatus()))
                .toList();
        return filterApprovalsByAuditScope(pending, currentUser).stream()
                .map(this::toView)
                .toList();
    }

    public List<ApprovalView> history(CurrentUser currentUser) {
        List<SysRegisterApproval> history = approvalMapper.selectList(new LambdaQueryWrapper<SysRegisterApproval>()
                .in(SysRegisterApproval::getApprovalStatus, List.of("APPROVED", "REJECTED"))
                .orderByDesc(SysRegisterApproval::getApprovedAt));
        history = history.stream()
                .filter(approval -> "APPROVED".equals(approval.getApprovalStatus())
                        || "REJECTED".equals(approval.getApprovalStatus()))
                .toList();
        return filterApprovalsByAuditScope(history, currentUser).stream()
                .map(this::toView)
                .toList();
    }

    private List<SysRegisterApproval> filterApprovalsByAuditScope(List<SysRegisterApproval> approvals, CurrentUser currentUser) {
        if (currentUser.superAdmin()) {
            return approvals;
        }
        requireAdmin(currentUser.userId());
        SysUser auditor = requireAuditor(currentUser.userId());
        return approvals.stream()
                .filter(approval -> {
                    SysUser user = userMapper.selectById(approval.getUserId());
                    return user != null && auditor.getDeptId() != null && auditor.getDeptId().equals(user.getDeptId());
                })
                .toList();
    }

    private ApprovalView toView(SysRegisterApproval approval) {
        SysUser user = userMapper.selectById(approval.getUserId());
        SysDept dept = user == null || user.getDeptId() == null ? null : deptMapper.selectById(user.getDeptId());
        SysUser approver = approval.getApproverId() == null ? null : userMapper.selectById(approval.getApproverId());
        return new ApprovalView(
                approval.getId(),
                user == null ? null : user.getRealName(),
                user == null ? null : user.getPhone(),
                dept == null ? null : dept.getDeptName(),
                approval.getApprovalStatus(),
                approval.getCreatedAt(),
                approval.getApprovedAt(),
                displayName(approver),
                approval.getRejectReason()
        );
    }

    private String displayName(SysUser user) {
        if (user == null) {
            return null;
        }
        if (user.getRealName() != null && !user.getRealName().isBlank()) {
            return user.getRealName();
        }
        return user.getPhone();
    }

    public SysUser approve(Long id, CurrentUser currentUser) {
        SysRegisterApproval approval = requireApproval(id);
        ensureCanAudit(approval, currentUser);
        approval.setApprovalStatus("APPROVED");
        approval.setApproverId(currentUser.userId());
        approval.setApprovedAt(LocalDateTime.now());
        approvalMapper.updateById(approval);

        SysUser user = requireTargetUser(approval);
        user.setApprovalStatus("APPROVED");
        user.setStatus("ENABLED");
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        assignStaffRoleIfMissing(user.getId());
        return user;
    }

    public SysUser reject(Long id, String reason, CurrentUser currentUser) {
        SysRegisterApproval approval = requireApproval(id);
        ensureCanAudit(approval, currentUser);
        approval.setApprovalStatus("REJECTED");
        approval.setRejectReason(reason);
        approval.setApproverId(currentUser.userId());
        approval.setApprovedAt(LocalDateTime.now());
        approvalMapper.updateById(approval);

        SysUser user = requireTargetUser(approval);
        user.setApprovalStatus("REJECTED");
        user.setStatus("DISABLED");
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return user;
    }

    private void ensureCanAudit(SysRegisterApproval approval, CurrentUser currentUser) {
        if (currentUser.superAdmin()) {
            return;
        }
        requireAdmin(currentUser.userId());
        SysUser auditor = requireAuditor(currentUser.userId());
        SysUser targetUser = requireTargetUser(approval);
        if (auditor.getDeptId() == null || !auditor.getDeptId().equals(targetUser.getDeptId())) {
            throw new BusinessException(403, "只能审核本科室或本车间的注册申请");
        }
    }

    private SysUser requireAuditor(Long userId) {
        SysUser auditor = userMapper.selectById(userId);
        if (auditor == null) {
            throw new BusinessException("审核人不存在");
        }
        return auditor;
    }

    private void requireAdmin(Long userId) {
        SysRole adminRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, UserAdminService.ADMIN_ROLE_CODE));
        if (adminRole == null) {
            throw new BusinessException(403, "没有注册审核权限");
        }
        Set<Long> adminUserIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getRoleId, adminRole.getId()))
                .stream()
                .map(SysUserRole::getUserId)
                .collect(Collectors.toSet());
        if (!adminUserIds.contains(userId)) {
            throw new BusinessException(403, "没有注册审核权限");
        }
    }

    private void assignStaffRoleIfMissing(Long userId) {
        List<SysUserRole> existingRoles = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
        if (!existingRoles.isEmpty()) {
            return;
        }
        SysRole staffRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, UserAdminService.STAFF_ROLE_CODE));
        if (staffRole == null) {
            throw new BusinessException("角色不存在：" + UserAdminService.STAFF_ROLE_CODE);
        }
        SysUserRole row = new SysUserRole();
        row.setUserId(userId);
        row.setRoleId(staffRole.getId());
        userRoleMapper.insert(row);
    }

    private SysRegisterApproval requireApproval(Long id) {
        SysRegisterApproval approval = approvalMapper.selectById(id);
        if (approval == null) {
            throw new BusinessException("审批记录不存在");
        }
        return approval;
    }

    private SysUser requireTargetUser(SysRegisterApproval approval) {
        SysUser user = userMapper.selectById(approval.getUserId());
        if (user == null) {
            throw new BusinessException("注册用户不存在");
        }
        return user;
    }
}
