package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.entity.SysDocAttachment;
import cn.datong.standard.entity.SysDocItemAttachment;
import cn.datong.standard.entity.SysDocSubmission;
import cn.datong.standard.security.SecurityUtils;
import cn.datong.standard.service.DocWorkspaceService;
import cn.datong.standard.storage.FileStorageService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
public class DocSubmissionController {
    private final DocWorkspaceService docWorkspaceService;
    private final FileStorageService storageService;

    @GetMapping("/api/submissions/{id}")
    public ApiResponse<SysDocSubmission> detail(@PathVariable Long id) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(docWorkspaceService.detail(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), id));
    }

    @GetMapping("/api/doc-attachments/{id}/download")
    public void download(@PathVariable Long id, HttpServletResponse response) throws Exception {
        CurrentUser currentUser = SecurityUtils.currentUser();
        SysDocAttachment attachment = docWorkspaceService.requireAttachment(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), id);
        downloadObject(response, attachment.getMimeType(), attachment.getOriginalFileName(), attachment.getStorageBucket(), attachment.getStoragePath());
    }

    @GetMapping("/api/doc-item-attachments/{id}/download")
    public void downloadItemAttachment(@PathVariable Long id, HttpServletResponse response) throws Exception {
        SysDocItemAttachment attachment = docWorkspaceService.requireItemAttachment(id);
        downloadObject(response, attachment.getMimeType(), attachment.getOriginalFileName(), attachment.getStorageBucket(), attachment.getStoragePath());
    }

    private void downloadObject(HttpServletResponse response,
                                String mimeType,
                                String originalFileName,
                                String storageBucket,
                                String storagePath) throws Exception {
        response.setContentType(mimeType == null ? "application/octet-stream" : mimeType);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''"
                + URLEncoder.encode(originalFileName, StandardCharsets.UTF_8));
        try (InputStream input = storageService.download(storageBucket, storagePath)) {
            StreamUtils.copy(input, response.getOutputStream());
        }
    }
}
