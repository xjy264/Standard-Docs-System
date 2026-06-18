package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.common.BusinessException;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.dto.ErrorEventListResponse;
import cn.datong.standard.entity.SysErrorEvent;
import cn.datong.standard.security.SecurityUtils;
import cn.datong.standard.service.ErrorEventService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/error-events")
@RequiredArgsConstructor
public class ErrorEventController {
    private final ErrorEventService errorEventService;

    @GetMapping
    public ApiResponse<ErrorEventListResponse> list(@RequestParam(required = false) String source,
                                                    @RequestParam(required = false) String severity,
                                                    @RequestParam(required = false) Boolean resolved,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                                                    @RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "20") Integer size) {
        requireSuperAdmin();
        return ApiResponse.success(errorEventService.list(source, severity, resolved, keyword, startTime, endTime, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<SysErrorEvent> detail(@PathVariable Long id) {
        requireSuperAdmin();
        return ApiResponse.success(errorEventService.detail(id));
    }

    @PostMapping("/frontend")
    public ApiResponse<Void> frontend(@RequestBody ErrorEventService.FrontendErrorReport report,
                                      HttpServletRequest request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        errorEventService.recordFrontendEvent(report, currentUser, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/resolve")
    public ApiResponse<Void> resolve(@PathVariable Long id, @RequestBody ResolveRequest request) {
        CurrentUser currentUser = requireSuperAdmin();
        errorEventService.resolve(id, currentUser.userId(), request == null ? null : request.remark());
        return ApiResponse.success();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String source,
                                         @RequestParam(required = false) String severity,
                                         @RequestParam(required = false) Boolean resolved,
                                         @RequestParam(required = false) Integer days) {
        requireSuperAdmin();
        byte[] bytes = errorEventService.exportZip(source, severity, resolved, days);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("standard-docs-error-events.zip")
                        .build()
                        .toString())
                .body(bytes);
    }

    private CurrentUser requireSuperAdmin() {
        CurrentUser currentUser = SecurityUtils.currentUser();
        if (!currentUser.superAdmin()) {
            throw new BusinessException(403, "只有超级管理员可以查看系统错误。");
        }
        return currentUser;
    }

    public record ResolveRequest(String remark) {
    }
}
