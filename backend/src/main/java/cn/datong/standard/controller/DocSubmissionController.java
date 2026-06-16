package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.dto.DocAttachmentPreview;
import cn.datong.standard.entity.SysDocAttachment;
import cn.datong.standard.entity.SysDocItemAttachment;
import cn.datong.standard.entity.SysDocSubmission;
import cn.datong.standard.security.SecurityUtils;
import cn.datong.standard.service.DocWorkspaceService;
import cn.datong.standard.storage.FileStorageService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    @Value("${app.office.onlyoffice-enabled:false}")
    private boolean onlyOfficeEnabled;
    @Value("${app.office.document-server-url:}")
    private String onlyOfficeUrl;

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
        CurrentUser currentUser = SecurityUtils.currentUser();
        SysDocItemAttachment attachment = docWorkspaceService.requireItemAttachment(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), id);
        downloadObject(response, attachment.getMimeType(), attachment.getOriginalFileName(), attachment.getStorageBucket(), attachment.getStoragePath());
    }

    @DeleteMapping("/api/doc-item-attachments/{id}")
    public ApiResponse<Void> deleteItemAttachment(@PathVariable Long id) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        docWorkspaceService.deleteItemAttachment(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), id);
        return ApiResponse.success();
    }

    @GetMapping("/api/doc-item-attachments/{id}/preview")
    public ApiResponse<DocAttachmentPreview> previewItemAttachment(@PathVariable Long id) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        SysDocItemAttachment attachment = docWorkspaceService.requireItemAttachment(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), id);
        String extension = attachment.getExtension() == null ? "" : attachment.getExtension().toLowerCase();
        if ("pdf".equals(extension)) {
            return ApiResponse.success(new DocAttachmentPreview("PDF", "PDF", attachment.getOriginalFileName(),
                    "/api/doc-item-attachments/" + id + "/inline", null, null));
        }
        if (isImageExtension(extension)) {
            return ApiResponse.success(new DocAttachmentPreview("IMAGE", extension.toUpperCase(), attachment.getOriginalFileName(),
                    "/api/doc-item-attachments/" + id + "/inline", null, null));
        }
        if (extension.matches("doc|docx|xls|xlsx|ppt|pptx")) {
            if (!onlyOfficeEnabled || onlyOfficeUrl == null || onlyOfficeUrl.isBlank()) {
                return ApiResponse.success(new DocAttachmentPreview("UNCONFIGURED", extension.toUpperCase(), attachment.getOriginalFileName(),
                        null, null, "预览服务未配置"));
            }
            return ApiResponse.success(new DocAttachmentPreview("ONLYOFFICE", extension.toUpperCase(), attachment.getOriginalFileName(),
                    "/api/doc-item-attachments/" + id + "/download", onlyOfficeUrl, null));
        }
        return ApiResponse.success(new DocAttachmentPreview("UNSUPPORTED", extension.toUpperCase(), attachment.getOriginalFileName(),
                null, null, "该格式暂不支持在线预览"));
    }

    @GetMapping("/api/doc-item-attachments/{id}/inline")
    public void inlineItemAttachment(@PathVariable Long id, HttpServletResponse response) throws Exception {
        CurrentUser currentUser = SecurityUtils.currentUser();
        SysDocItemAttachment attachment = docWorkspaceService.requireItemAttachment(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), id);
        downloadObject(response, attachment.getMimeType(), attachment.getOriginalFileName(), attachment.getStorageBucket(), attachment.getStoragePath(), true);
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

    private void downloadObject(HttpServletResponse response,
                                String mimeType,
                                String originalFileName,
                                String storageBucket,
                                String storagePath,
                                boolean inline) throws Exception {
        response.setContentType(mimeType == null ? "application/octet-stream" : mimeType);
        response.setHeader("Content-Disposition", (inline ? "inline" : "attachment") + "; filename*=UTF-8''"
                + URLEncoder.encode(originalFileName, StandardCharsets.UTF_8));
        try (InputStream input = storageService.download(storageBucket, storagePath)) {
            StreamUtils.copy(input, response.getOutputStream());
        }
    }

    private boolean isImageExtension(String extension) {
        return extension.matches("png|jpg|jpeg|gif|bmp|webp|svg");
    }
}
