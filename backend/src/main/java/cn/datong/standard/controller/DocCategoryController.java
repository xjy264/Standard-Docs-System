package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.entity.SysDocCategory;
import cn.datong.standard.entity.SysDocSubmission;
import cn.datong.standard.security.SecurityUtils;
import cn.datong.standard.service.DocWorkspaceService;
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
@RequestMapping("/api/doc-categories")
@RequiredArgsConstructor
public class DocCategoryController {
    private final DocWorkspaceService docWorkspaceService;

    @GetMapping
    public ApiResponse<List<SysDocCategory>> list(@RequestParam Long sectionDeptId) {
        return ApiResponse.success(docWorkspaceService.categories(sectionDeptId));
    }

    @PostMapping
    public ApiResponse<SysDocCategory> create(@RequestBody SysDocCategory category) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(docWorkspaceService.createCategory(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), category));
    }

    @PutMapping("/{id}")
    public ApiResponse<SysDocCategory> update(@PathVariable Long id, @RequestBody SysDocCategory category) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(docWorkspaceService.updateCategory(currentUser.deptId(), currentUser.superAdmin(), id, category));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        docWorkspaceService.deleteCategory(currentUser.deptId(), currentUser.superAdmin(), id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/submissions")
    public ApiResponse<List<SysDocSubmission>> submissions(@PathVariable Long id) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(docWorkspaceService.submissions(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), id));
    }
}
