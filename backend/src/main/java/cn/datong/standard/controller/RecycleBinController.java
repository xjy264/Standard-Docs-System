package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.entity.SysRecycleBin;
import cn.datong.standard.mapper.SysRecycleBinMapper;
import cn.datong.standard.security.SecurityUtils;
import cn.datong.standard.service.FileService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recycle-bin")
@RequiredArgsConstructor
public class RecycleBinController {
    private final SysRecycleBinMapper recycleBinMapper;
    private final FileService fileService;

    @GetMapping
    public ApiResponse<List<SysRecycleBin>> list() {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(recycleBinMapper.selectList(new LambdaQueryWrapper<SysRecycleBin>()
                .eq(SysRecycleBin::getStatus, "ACTIVE")
                .eq(SysRecycleBin::getDeletedBy, currentUser.userId())
                .orderByDesc(SysRecycleBin::getDeletedAt)));
    }

    @PostMapping("/{fileId}/restore")
    public ApiResponse<Void> restore(@PathVariable Long fileId, HttpServletRequest request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        fileService.restore(currentUser.userId(), currentUser.superAdmin(), fileId, request);
        return ApiResponse.success();
    }
}
