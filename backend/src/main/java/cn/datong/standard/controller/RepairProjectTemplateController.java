package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.dto.RepairProjectImportRequest;
import cn.datong.standard.entity.SysDocNode;
import cn.datong.standard.entity.SysRepairProjectTemplate;
import cn.datong.standard.entity.SysRepairProjectTemplateItem;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/repair-project-templates")
@RequiredArgsConstructor
public class RepairProjectTemplateController {
    private final DocWorkspaceService docWorkspaceService;

    @GetMapping
    public ApiResponse<List<SysRepairProjectTemplate>> list() {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(docWorkspaceService.repairProjectTemplates(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin()));
    }

    @PostMapping
    public ApiResponse<SysRepairProjectTemplate> create(@RequestBody SysRepairProjectTemplate request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(docWorkspaceService.saveRepairProjectTemplate(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<SysRepairProjectTemplate> update(@PathVariable Long id, @RequestBody SysRepairProjectTemplate request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        request.setId(id);
        return ApiResponse.success(docWorkspaceService.saveRepairProjectTemplate(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        docWorkspaceService.deleteRepairProjectTemplate(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/items")
    public ApiResponse<List<SysRepairProjectTemplateItem>> items(@PathVariable Long id) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(docWorkspaceService.repairProjectTemplateItems(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), id));
    }

    @PostMapping("/{id}/items")
    public ApiResponse<SysRepairProjectTemplateItem> createItem(@PathVariable Long id, @RequestBody SysRepairProjectTemplateItem request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(docWorkspaceService.saveRepairProjectTemplateItem(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), id, request));
    }

    @PutMapping("/{templateId}/items/{itemId}")
    public ApiResponse<SysRepairProjectTemplateItem> updateItem(@PathVariable Long templateId,
                                                                @PathVariable Long itemId,
                                                                @RequestBody SysRepairProjectTemplateItem request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        request.setId(itemId);
        return ApiResponse.success(docWorkspaceService.saveRepairProjectTemplateItem(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), templateId, request));
    }

    @DeleteMapping("/{templateId}/items/{itemId}")
    public ApiResponse<Void> deleteItem(@PathVariable Long templateId, @PathVariable Long itemId) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        docWorkspaceService.deleteRepairProjectTemplateItem(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), itemId);
        return ApiResponse.success();
    }

    @PostMapping("/import/{parentNodeId}")
    public ApiResponse<SysDocNode> importTemplate(@PathVariable Long parentNodeId, @RequestBody RepairProjectImportRequest request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(docWorkspaceService.importRepairProjectTemplate(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(),
                parentNodeId, request.templateId(), request.projectFolderName(), request.docYear()));
    }
}
