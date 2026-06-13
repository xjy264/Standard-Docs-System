package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.dto.DocNodeRequest;
import cn.datong.standard.entity.SysDocNode;
import cn.datong.standard.security.SecurityUtils;
import cn.datong.standard.service.DocWorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class DocNodeController {
    private final DocWorkspaceService docWorkspaceService;

    @GetMapping("/api/doc-tree")
    public ApiResponse<List<SysDocNode>> tree(@RequestParam Long sectionDeptId,
                                              @RequestParam(required = false) String businessType) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(docWorkspaceService.documentTree(currentUser.userId(), currentUser.deptId(),
                currentUser.superAdmin(), sectionDeptId, businessType));
    }

    @PostMapping("/api/doc-nodes/folders")
    public ApiResponse<SysDocNode> createFolder(@RequestBody DocNodeRequest request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(docWorkspaceService.createFolderNode(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), request));
    }

    @PostMapping(value = "/api/doc-nodes/files", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<SysDocNode> createFile(@RequestBody DocNodeRequest request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(docWorkspaceService.createFileNode(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), request));
    }

    @PostMapping(value = "/api/doc-nodes/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SysDocNode> createFileWithMainFile(@RequestParam Long sectionDeptId,
                                                          @RequestParam Long parentId,
                                                          @RequestParam String nodeName,
                                                          @RequestParam(required = false) Integer sortOrder,
                                                          @RequestParam Integer docYear,
                                                          @RequestParam(required = false, defaultValue = "false") Boolean workshopUploadEnabled,
                                                          @RequestParam(required = false)
                                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                          LocalDateTime uploadDeadline,
                                                          @RequestParam(required = false) List<Long> visibleWorkshopIds,
                                                          @RequestParam MultipartFile file) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        DocNodeRequest request = new DocNodeRequest(sectionDeptId, parentId, nodeName, sortOrder, null,
                workshopUploadEnabled, "", null, docYear, workshopUploadEnabled ? "UPLOAD" : "ISSUED",
                workshopUploadEnabled ? "MULTIPLE" : "SINGLE", null, uploadDeadline,
                workshopUploadEnabled, visibleWorkshopIds);
        return ApiResponse.success(docWorkspaceService.createFileNodeWithMainFile(currentUser.userId(), currentUser.deptId(),
                currentUser.superAdmin(), request, file));
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
