package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.entity.SysFile;
import cn.datong.standard.mapper.SysFileMapper;
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
    private final SysFileMapper fileMapper;
    private final PermissionService permissionService;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        CurrentUser currentUser = SecurityUtils.currentUser();
        permissionService.require(currentUser.userId(), currentUser.superAdmin(), "storage:view");
        var files = fileMapper.selectList(new LambdaQueryWrapper<SysFile>().eq(SysFile::getDeleted, 0));
        long totalSize = files.stream().map(SysFile::getFileSize).filter(size -> size != null).mapToLong(Long::longValue).sum();
        return ApiResponse.success(Map.of("fileCount", files.size(), "totalSize", totalSize));
    }
}
