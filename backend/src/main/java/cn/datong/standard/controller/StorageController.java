package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.entity.SysDocAttachment;
import cn.datong.standard.mapper.SysDocAttachmentMapper;
import cn.datong.standard.security.SecurityUtils;
import cn.datong.standard.service.PermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {
    private final SysDocAttachmentMapper attachmentMapper;
    private final PermissionService permissionService;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        CurrentUser currentUser = SecurityUtils.currentUser();
        permissionService.require(currentUser.userId(), currentUser.superAdmin(), "storage:view");
        var attachments = attachmentMapper.selectList(new LambdaQueryWrapper<SysDocAttachment>());
        long totalSize = attachments.stream().map(SysDocAttachment::getFileSize).filter(size -> size != null).mapToLong(Long::longValue).sum();
        return ApiResponse.success(Map.of("fileCount", attachments.size(), "totalSize", totalSize));
    }
}
