package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.entity.SysFolder;
import cn.datong.standard.security.SecurityUtils;
import cn.datong.standard.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {
    private final FolderService folderService;

    @GetMapping
    public ApiResponse<List<SysFolder>> list(@RequestParam Long deptId) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(folderService.list(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), deptId));
    }

    @PostMapping
    public ApiResponse<SysFolder> create(@RequestBody SysFolder folder) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(folderService.create(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), folder));
    }

    @PutMapping("/{id}")
    public ApiResponse<SysFolder> update(@PathVariable Long id, @RequestBody SysFolder folder) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(folderService.update(currentUser.deptId(), currentUser.superAdmin(), id, folder));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        folderService.delete(currentUser.deptId(), currentUser.superAdmin(), id);
        return ApiResponse.success();
    }
}
