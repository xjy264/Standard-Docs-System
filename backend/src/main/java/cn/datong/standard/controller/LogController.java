package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.entity.SysLoginLog;
import cn.datong.standard.entity.SysOperationLog;
import cn.datong.standard.mapper.SysLoginLogMapper;
import cn.datong.standard.mapper.SysOperationLogMapper;
import cn.datong.standard.security.SecurityUtils;
import cn.datong.standard.service.PermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {
    private final SysOperationLogMapper operationLogMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final PermissionService permissionService;

    @GetMapping("/operations")
    public ApiResponse<List<SysOperationLog>> operations() {
        CurrentUser currentUser = SecurityUtils.currentUser();
        permissionService.require(currentUser.userId(), currentUser.superAdmin(), "log:view");
        return ApiResponse.success(operationLogMapper.selectList(new LambdaQueryWrapper<SysOperationLog>()
                .orderByDesc(SysOperationLog::getCreatedAt)));
    }

    @GetMapping("/logins")
    public ApiResponse<List<SysLoginLog>> logins() {
        CurrentUser currentUser = SecurityUtils.currentUser();
        permissionService.require(currentUser.userId(), currentUser.superAdmin(), "log:view");
        return ApiResponse.success(loginLogMapper.selectList(new LambdaQueryWrapper<SysLoginLog>()
                .orderByDesc(SysLoginLog::getCreatedAt)));
    }
}
