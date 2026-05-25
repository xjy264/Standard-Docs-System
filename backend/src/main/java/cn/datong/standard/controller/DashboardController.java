package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.entity.SysDept;
import cn.datong.standard.entity.SysDocAttachment;
import cn.datong.standard.entity.SysDocItem;
import cn.datong.standard.entity.SysDocSubmission;
import cn.datong.standard.entity.SysNotification;
import cn.datong.standard.entity.SysRegisterApproval;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.mapper.SysDeptMapper;
import cn.datong.standard.mapper.SysDocAttachmentMapper;
import cn.datong.standard.mapper.SysDocItemMapper;
import cn.datong.standard.mapper.SysDocSubmissionMapper;
import cn.datong.standard.mapper.SysNotificationMapper;
import cn.datong.standard.mapper.SysRegisterApprovalMapper;
import cn.datong.standard.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final SysUserMapper userMapper;
    private final SysDocItemMapper docItemMapper;
    private final SysNotificationMapper notificationMapper;
    private final SysDocSubmissionMapper submissionMapper;
    private final SysDocAttachmentMapper attachmentMapper;
    private final SysRegisterApprovalMapper approvalMapper;
    private final SysDeptMapper deptMapper;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Long>> stats() {
        Long userCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeleted, 0));
        Long fileCount = docItemMapper.selectCount(new LambdaQueryWrapper<SysDocItem>().eq(SysDocItem::getDeleted, 0));
        Long submissionCount = submissionMapper.selectCount(new LambdaQueryWrapper<SysDocSubmission>());
        Long attachmentCount = attachmentMapper.selectCount(new LambdaQueryWrapper<SysDocAttachment>());
        Long pendingApprovalCount = approvalMapper.selectCount(new LambdaQueryWrapper<SysRegisterApproval>()
                .eq(SysRegisterApproval::getApprovalStatus, "PENDING"));
        Long unreadCount = notificationMapper.selectCount(new LambdaQueryWrapper<SysNotification>().eq(SysNotification::getReadStatus, "UNREAD"));
        Long uploadEnabledFileCount = docItemMapper.selectCount(new LambdaQueryWrapper<SysDocItem>()
                .eq(SysDocItem::getDeleted, 0)
                .eq(SysDocItem::getAttachmentEnabled, 1));
        Long sectionCount = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeleted, 0)
                .eq(SysDept::getDeptType, "SECTION"));
        return ApiResponse.success(Map.of(
                "userCount", userCount,
                "fileCount", fileCount,
                "submissionCount", submissionCount,
                "attachmentCount", attachmentCount,
                "pendingApprovalCount", pendingApprovalCount,
                "unreadCount", unreadCount,
                "uploadEnabledFileCount", uploadEnabledFileCount,
                "sectionCount", sectionCount
        ));
    }
}
