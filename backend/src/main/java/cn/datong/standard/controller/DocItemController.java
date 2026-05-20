package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.entity.SysDocField;
import cn.datong.standard.entity.SysDocItem;
import cn.datong.standard.entity.SysDocSubmission;
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
@RequestMapping("/api/doc-items")
@RequiredArgsConstructor
public class DocItemController {
    private final DocWorkspaceService docWorkspaceService;

    @GetMapping
    public ApiResponse<List<SysDocItem>> list(@RequestParam Long categoryId) {
        return ApiResponse.success(docWorkspaceService.items(categoryId));
    }

    @GetMapping("/{id}")
    public ApiResponse<SysDocItem> detail(@PathVariable Long id) {
        return ApiResponse.success(docWorkspaceService.item(id));
    }

    @PostMapping
    public ApiResponse<SysDocItem> create(@RequestBody SysDocItem item) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(docWorkspaceService.createItem(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), item));
    }

    @PutMapping("/{id}")
    public ApiResponse<SysDocItem> update(@PathVariable Long id, @RequestBody SysDocItem item) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(docWorkspaceService.updateItem(currentUser.deptId(), currentUser.superAdmin(), id, item));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        docWorkspaceService.deleteItem(currentUser.deptId(), currentUser.superAdmin(), id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/fields")
    public ApiResponse<List<SysDocField>> fields(@PathVariable Long id) {
        return ApiResponse.success(docWorkspaceService.fields(id));
    }

    @PutMapping("/{id}/fields")
    public ApiResponse<List<SysDocField>> saveFields(@PathVariable Long id, @RequestBody List<SysDocField> fields) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(docWorkspaceService.saveFields(currentUser.deptId(), currentUser.superAdmin(), id, fields));
    }

    @PostMapping(value = "/{id}/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SysDocSubmission> submit(@PathVariable Long id,
                                                @RequestParam(required = false) String valuesJson,
                                                @RequestParam(required = false) List<MultipartFile> files) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(docWorkspaceService.submit(currentUser.userId(), currentUser.deptId(), id, valuesJson, files));
    }
}
