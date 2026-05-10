package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.entity.SysSystemConfig;
import cn.datong.standard.mapper.SysSystemConfigMapper;
import cn.datong.standard.security.SecurityUtils;
import cn.datong.standard.service.PermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/system-config")
@RequiredArgsConstructor
public class SystemConfigController {
    private final SysSystemConfigMapper configMapper;
    private final PermissionService permissionService;

    @GetMapping
    public ApiResponse<List<SysSystemConfig>> list() {
        CurrentUser currentUser = SecurityUtils.currentUser();
        permissionService.require(currentUser.userId(), currentUser.superAdmin(), "system:manage");
        return ApiResponse.success(configMapper.selectList(new LambdaQueryWrapper<>()));
    }

    @PostMapping
    public ApiResponse<Void> save(@RequestBody List<SysSystemConfig> configs) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        permissionService.require(currentUser.userId(), currentUser.superAdmin(), "system:manage");
        for (SysSystemConfig config : configs) {
            config.setUpdatedAt(LocalDateTime.now());
            if (config.getId() == null) {
                config.setCreatedAt(LocalDateTime.now());
                configMapper.insert(config);
            } else {
                configMapper.updateById(config);
            }
        }
        return ApiResponse.success();
    }
}
