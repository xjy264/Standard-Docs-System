package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.dto.DocNodeRequest;
import cn.datong.standard.entity.SysDocNode;
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
@RequiredArgsConstructor
public class DocNodeController {
    private final DocWorkspaceService docWorkspaceService;

    @GetMapping("/api/doc-tree")
    public ApiResponse<List<SysDocNode>> tree(@RequestParam Long sectionDeptId) {
        return ApiResponse.success(docWorkspaceService.documentTree(sectionDeptId));
    }

    @PostMapping("/api/doc-nodes/folders")
    public ApiResponse<SysDocNode> createFolder(@RequestBody DocNodeRequest request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(docWorkspaceService.createFolderNode(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), request));
    }

    @PostMapping("/api/doc-nodes/files")
    public ApiResponse<SysDocNode> createFile(@RequestBody DocNodeRequest request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(docWorkspaceService.createFileNode(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), request));
    }

    @PutMapping("/api/doc-nodes/{id}")
    public ApiResponse<SysDocNode> update(@PathVariable Long id, @RequestBody DocNodeRequest request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(docWorkspaceService.updateNode(currentUser.deptId(), currentUser.superAdmin(), id, request));
    }

    @DeleteMapping("/api/doc-nodes/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        docWorkspaceService.deleteNode(currentUser.deptId(), currentUser.superAdmin(), id);
        return ApiResponse.success();
    }
}
