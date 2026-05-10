package cn.datong.standard.controller;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.dto.FileSearchRequest;
import cn.datong.standard.entity.SysFile;
import cn.datong.standard.enums.VisibilityScope;
import cn.datong.standard.security.SecurityUtils;
import cn.datong.standard.service.FileService;
import cn.datong.standard.storage.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;
    private final FileStorageService storageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SysFile> upload(@RequestParam MultipartFile file,
                                       @RequestParam(required = false) Long folderId,
                                       @RequestParam(required = false) VisibilityScope visibilityScope,
                                       @RequestParam(required = false) List<Long> userIds,
                                       @RequestParam(required = false) List<Long> deptIds,
                                       HttpServletRequest request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(fileService.upload(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(),
                file, folderId, visibilityScope, userIds, deptIds, request));
    }

    @GetMapping
    public ApiResponse<List<SysFile>> list(@ModelAttribute FileSearchRequest request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(fileService.search(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), request));
    }

    @GetMapping("/{id}")
    public ApiResponse<SysFile> detail(@PathVariable Long id) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(fileService.detail(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), id));
    }

    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CurrentUser currentUser = SecurityUtils.currentUser();
        SysFile file = fileService.requireDownload(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), id, request);
        response.setContentType(file.getMimeType() == null ? "application/octet-stream" : file.getMimeType());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''"
                + URLEncoder.encode(file.getOriginalFileName(), StandardCharsets.UTF_8));
        try (InputStream input = storageService.download(file.getStorageBucket(), file.getStoragePath())) {
            StreamUtils.copy(input, response.getOutputStream());
        }
    }

    @GetMapping("/{id}/preview")
    public ApiResponse<Void> preview(@PathVariable Long id) {
        throw new BusinessException(410, "预览功能已下线，请下载文件查看");
    }

    @GetMapping("/{id}/office-config")
    public ApiResponse<Void> officeConfig(@PathVariable Long id) {
        throw new BusinessException(410, "在线编辑功能已下线，请在个人空间替换文件");
    }

    @PostMapping(value = "/{id}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SysFile> replace(@PathVariable Long id,
                                        @RequestParam MultipartFile file,
                                        HttpServletRequest request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(fileService.replace(currentUser.userId(), currentUser.superAdmin(), id, file, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        fileService.softDelete(currentUser.userId(), currentUser.deptId(), currentUser.superAdmin(), id, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/restore")
    public ApiResponse<Void> restore(@PathVariable Long id, HttpServletRequest request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        fileService.restore(currentUser.userId(), currentUser.superAdmin(), id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}/remove")
    public ApiResponse<Void> remove(@PathVariable Long id, HttpServletRequest request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        fileService.remove(currentUser.userId(), currentUser.superAdmin(), id, request);
        return ApiResponse.success();
    }
}
