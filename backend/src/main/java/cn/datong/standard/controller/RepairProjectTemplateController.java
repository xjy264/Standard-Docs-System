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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping("/items")
    public ApiResponse<List<SysRepairProjectTemplateItem>> libraryItems() {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(docWorkspaceService.repairProjectTemplateItems(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin()));
    }

    @PostMapping(value = "/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SysRepairProjectTemplateItem> createLibraryItemWithFile(@RequestParam String itemName,
                                                                               @RequestParam(required = false) Integer sortOrder,
                                                                               @RequestParam MultipartFile file) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        SysRepairProjectTemplateItem request = new SysRepairProjectTemplateItem();
        request.setItemName(itemName);
        request.setSortOrder(sortOrder);
        return ApiResponse.success(docWorkspaceService.saveRepairProjectTemplateItemWithFile(currentUser.userId(), currentUser.deptId(),
                currentUser.superAdmin(), request, file));
    }

    @PutMapping(value = "/items/{itemId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<SysRepairProjectTemplateItem> updateLibraryItem(@PathVariable Long itemId,
                                                                       @RequestBody SysRepairProjectTemplateItem request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        request.setId(itemId);
        return ApiResponse.success(docWorkspaceService.saveRepairProjectTemplateItem(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), request));
    }

    @PutMapping(value = "/items/{itemId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SysRepairProjectTemplateItem> updateLibraryItemWithFile(@PathVariable Long itemId,
                                                                               @RequestParam String itemName,
                                                                               @RequestParam(required = false) Integer sortOrder,
                                                                               @RequestParam MultipartFile file) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        SysRepairProjectTemplateItem request = new SysRepairProjectTemplateItem();
        request.setId(itemId);
        request.setItemName(itemName);
        request.setSortOrder(sortOrder);
        return ApiResponse.success(docWorkspaceService.saveRepairProjectTemplateItemWithFile(currentUser.userId(), currentUser.deptId(),
                currentUser.superAdmin(), request, file));
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Void> deleteLibraryItem(@PathVariable Long itemId) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        docWorkspaceService.deleteRepairProjectTemplateItem(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), itemId);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/items")
    public ApiResponse<List<SysRepairProjectTemplateItem>> items(@PathVariable Long id) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(docWorkspaceService.repairProjectTemplateItems(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), id));
    }

    @PostMapping(value = "/{id}/items", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<SysRepairProjectTemplateItem> createItem(@PathVariable Long id, @RequestBody SysRepairProjectTemplateItem request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(docWorkspaceService.saveRepairProjectTemplateItem(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), id, request));
    }

    @PostMapping(value = "/{id}/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SysRepairProjectTemplateItem> createItemWithFile(@PathVariable Long id,
                                                                        @RequestParam String itemName,
                                                                        @RequestParam(required = false) Integer sortOrder,
                                                                        @RequestParam MultipartFile file) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        SysRepairProjectTemplateItem request = new SysRepairProjectTemplateItem();
        request.setItemName(itemName);
        request.setSortOrder(sortOrder);
        return ApiResponse.success(docWorkspaceService.saveRepairProjectTemplateItemWithFile(currentUser.userId(), currentUser.deptId(),
                currentUser.superAdmin(), id, request, file));
    }

    @PutMapping(value = "/{templateId}/items/{itemId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<SysRepairProjectTemplateItem> updateItem(@PathVariable Long templateId,
                                                                @PathVariable Long itemId,
                                                                @RequestBody SysRepairProjectTemplateItem request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        request.setId(itemId);
        return ApiResponse.success(docWorkspaceService.saveRepairProjectTemplateItem(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), templateId, request));
    }

    @PutMapping(value = "/{templateId}/items/{itemId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SysRepairProjectTemplateItem> updateItemWithFile(@PathVariable Long templateId,
                                                                        @PathVariable Long itemId,
                                                                        @RequestParam String itemName,
                                                                        @RequestParam(required = false) Integer sortOrder,
                                                                        @RequestParam MultipartFile file) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        SysRepairProjectTemplateItem request = new SysRepairProjectTemplateItem();
        request.setId(itemId);
        request.setItemName(itemName);
        request.setSortOrder(sortOrder);
        return ApiResponse.success(docWorkspaceService.saveRepairProjectTemplateItemWithFile(currentUser.userId(), currentUser.deptId(),
                currentUser.superAdmin(), templateId, request, file));
    }

    @DeleteMapping("/{templateId}/items/{itemId}")
    public ApiResponse<Void> deleteItem(@PathVariable Long templateId, @PathVariable Long itemId) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        docWorkspaceService.deleteRepairProjectTemplateItem(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), itemId);
        return ApiResponse.success();
    }

    @PostMapping("/import/{parentNodeId}")
    public ApiResponse<List<SysDocNode>> importTemplate(@PathVariable Long parentNodeId, @RequestBody RepairProjectImportRequest request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(docWorkspaceService.importRepairTemplateFiles(currentUser.userId(), currentUser.deptId(),
                currentUser.superAdmin(), parentNodeId, request.templateItemIds(), request.docYear()));
    }
}
