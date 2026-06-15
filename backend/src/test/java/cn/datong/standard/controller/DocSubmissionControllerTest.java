package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.dto.DocAttachmentPreview;
import cn.datong.standard.entity.SysDocItemAttachment;
import cn.datong.standard.service.DocWorkspaceService;
import cn.datong.standard.storage.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocSubmissionControllerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void previewItemAttachmentSupportsImageInlinePreview() {
        DocWorkspaceService docWorkspaceService = mock(DocWorkspaceService.class);
        DocSubmissionController controller = new DocSubmissionController(docWorkspaceService, mock(FileStorageService.class));
        CurrentUser currentUser = new CurrentUser(1L, 2L, true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, List.of()));
        SysDocItemAttachment attachment = new SysDocItemAttachment();
        attachment.setId(7L);
        attachment.setOriginalFileName("现场照片.png");
        attachment.setExtension("png");
        attachment.setMimeType("image/png");
        when(docWorkspaceService.requireItemAttachment(1L, 2L, true, 7L)).thenReturn(attachment);

        ApiResponse<DocAttachmentPreview> response = controller.previewItemAttachment(7L);

        assertThat(response.getData().previewType()).isEqualTo("IMAGE");
        assertThat(response.getData().fileType()).isEqualTo("PNG");
        assertThat(response.getData().url()).isEqualTo("/api/doc-item-attachments/7/inline");
    }

    @Test
    void previewItemAttachmentReturnsOnlyOfficeConfigWhenOfficePreviewEnabled() {
        DocWorkspaceService docWorkspaceService = mock(DocWorkspaceService.class);
        DocSubmissionController controller = new DocSubmissionController(docWorkspaceService, mock(FileStorageService.class));
        ReflectionTestUtils.setField(controller, "onlyOfficeEnabled", true);
        ReflectionTestUtils.setField(controller, "onlyOfficeUrl", "http://localhost:8082");
        CurrentUser currentUser = new CurrentUser(1L, 2L, true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, List.of()));
        SysDocItemAttachment attachment = new SysDocItemAttachment();
        attachment.setId(8L);
        attachment.setOriginalFileName("统计表.xlsx");
        attachment.setExtension("xlsx");
        attachment.setMimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        when(docWorkspaceService.requireItemAttachment(1L, 2L, true, 8L)).thenReturn(attachment);

        ApiResponse<DocAttachmentPreview> response = controller.previewItemAttachment(8L);

        assertThat(response.getData().previewType()).isEqualTo("ONLYOFFICE");
        assertThat(response.getData().fileType()).isEqualTo("XLSX");
        assertThat(response.getData().url()).isEqualTo("/api/doc-item-attachments/8/download");
        assertThat(response.getData().documentServerUrl()).isEqualTo("http://localhost:8082");
    }
}
