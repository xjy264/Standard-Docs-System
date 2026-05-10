package cn.datong.standard.service;

import cn.datong.standard.dto.FileSearchRequest;
import cn.datong.standard.entity.SysFile;
import cn.datong.standard.enums.VisibilityScope;
import cn.datong.standard.mapper.SysFileMapper;
import cn.datong.standard.mapper.SysFilePermissionMapper;
import cn.datong.standard.mapper.SysRecycleBinMapper;
import cn.datong.standard.storage.FileStorageService;
import cn.datong.standard.storage.StoredObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileServiceTest {

    @Test
    void docSearchIncludesDocAndDocx() {
        assertThat(FileService.searchExtensions("doc")).containsExactly("doc", "docx");
    }

    @ParameterizedTest
    @CsvSource({
            "xls, xls|xlsx",
            "ppt, ppt|pptx",
            "dwg, dwg|dxf",
            "jpg, jpg|jpeg|png",
            "zip, zip|rar|7z",
            "pdf, pdf"
    })
    void commonTypeSearchIncludesGroupedExtensions(String extension, String expected) {
        assertThat(FileService.searchExtensions(extension)).containsExactly(expected.split("\\|"));
    }

    @Test
    void searchReturnsOnlyAccessibleFiles() {
        SysFileMapper fileMapper = mock(SysFileMapper.class);
        FileAccessService fileAccessService = mock(FileAccessService.class);
        SysFile hidden = SysFile.builder()
                .id(1L)
                .fileName("未授权文件.docx")
                .visibilityScope(VisibilityScope.ASSIGNED)
                .build();
        SysFile visible = SysFile.builder()
                .id(2L)
                .fileName("可见文件.pdf")
                .visibilityScope(VisibilityScope.PUBLIC)
                .build();
        when(fileMapper.selectList(any())).thenReturn(List.of(hidden, visible));
        when(fileAccessService.canAccess(30L, 21L, false, 1L)).thenReturn(false);
        when(fileAccessService.canAccess(30L, 21L, false, 2L)).thenReturn(true);
        FileService service = new FileService(
                fileMapper,
                mock(SysFilePermissionMapper.class),
                mock(SysRecycleBinMapper.class),
                mock(FileStorageService.class),
                fileAccessService,
                mock(PermissionService.class),
                mock(OperationLogService.class)
        );

        List<SysFile> result = service.search(30L, 21L, false,
                new FileSearchRequest(null, null, null, null, null, null, null, false));

        assertThat(result).containsExactly(visible);
    }

    @Test
    void mineSearchReturnsOnlyCurrentUsersUploads() {
        SysFileMapper fileMapper = mock(SysFileMapper.class);
        FileAccessService fileAccessService = mock(FileAccessService.class);
        SysFile own = SysFile.builder()
                .id(1L)
                .uploadUserId(30L)
                .fileName("我的文件.pdf")
                .build();
        SysFile other = SysFile.builder()
                .id(2L)
                .uploadUserId(31L)
                .fileName("他人文件.pdf")
                .build();
        when(fileMapper.selectList(any())).thenReturn(List.of(own, other));
        when(fileAccessService.canAccess(30L, 21L, false, 1L)).thenReturn(true);
        when(fileAccessService.canAccess(30L, 21L, false, 2L)).thenReturn(true);
        FileService service = new FileService(
                fileMapper,
                mock(SysFilePermissionMapper.class),
                mock(SysRecycleBinMapper.class),
                mock(FileStorageService.class),
                fileAccessService,
                mock(PermissionService.class),
                mock(OperationLogService.class)
        );

        List<SysFile> result = service.search(30L, 21L, false,
                new FileSearchRequest(null, null, null, null, null, null, null, true));

        assertThat(result).containsExactly(own);
    }

    @Test
    void ownerCanSoftDeleteAnyOwnFile() {
        SysFileMapper fileMapper = mock(SysFileMapper.class);
        SysFile file = SysFile.builder()
                .id(1L)
                .uploadUserId(10L)
                .deptId(7L)
                .visibilityScope(VisibilityScope.PUBLIC)
                .storagePath("docs/a.pdf")
                .fileSize(12L)
                .build();
        when(fileMapper.selectById(1L)).thenReturn(file);
        FileService service = fileService(fileMapper, mock(SysRecycleBinMapper.class), mock(FileStorageService.class));

        service.softDelete(10L, 7L, false, 1L, null);

        verify(fileMapper).updateById(file);
        assertThat(file.getDeleted()).isEqualTo(1);
    }

    @Test
    void superAdminCannotSoftDeleteOthersFile() {
        SysFileMapper fileMapper = mock(SysFileMapper.class);
        when(fileMapper.selectById(1L)).thenReturn(SysFile.builder()
                .id(1L)
                .uploadUserId(10L)
                .deptId(7L)
                .build());
        FileService service = fileService(fileMapper, mock(SysRecycleBinMapper.class), mock(FileStorageService.class));

        assertThatThrownBy(() -> service.softDelete(1L, 24L, true, 1L, null))
                .isInstanceOf(cn.datong.standard.common.BusinessException.class)
                .hasMessage("只有文件所有者可以改动删除该文件");
    }

    @Test
    void ownerCanRestoreOwnFile() {
        SysFileMapper fileMapper = mock(SysFileMapper.class);
        SysFile file = SysFile.builder()
                .id(1L)
                .uploadUserId(10L)
                .deleted(1)
                .build();
        when(fileMapper.selectById(1L)).thenReturn(file);
        FileService service = fileService(fileMapper, mock(SysRecycleBinMapper.class), mock(FileStorageService.class));

        service.restore(10L, true, 1L, null);

        verify(fileMapper).updateById(file);
        assertThat(file.getDeleted()).isZero();
    }

    @Test
    void nonOwnerCannotRestoreFileEvenWithSuperAdminFlag() {
        SysFileMapper fileMapper = mock(SysFileMapper.class);
        when(fileMapper.selectById(1L)).thenReturn(SysFile.builder()
                .id(1L)
                .uploadUserId(10L)
                .deleted(1)
                .build());
        FileService service = fileService(fileMapper, mock(SysRecycleBinMapper.class), mock(FileStorageService.class));

        assertThatThrownBy(() -> service.restore(1L, true, 1L, null))
                .isInstanceOf(cn.datong.standard.common.BusinessException.class)
                .hasMessage("只有文件所有者可以改动删除该文件");
    }

    @Test
    void nonOwnerCannotRemoveFileEvenWithSuperAdminFlag() {
        SysFileMapper fileMapper = mock(SysFileMapper.class);
        when(fileMapper.selectById(1L)).thenReturn(SysFile.builder()
                .id(1L)
                .uploadUserId(10L)
                .storageBucket("bucket")
                .storagePath("docs/a.pdf")
                .build());
        FileService service = fileService(fileMapper, mock(SysRecycleBinMapper.class), mock(FileStorageService.class));

        assertThatThrownBy(() -> service.remove(1L, true, 1L, null))
                .isInstanceOf(cn.datong.standard.common.BusinessException.class)
                .hasMessage("只有文件所有者可以改动删除该文件");
    }

    @Test
    void nonOwnerCannotUseEditEntry() {
        SysFileMapper fileMapper = mock(SysFileMapper.class);
        when(fileMapper.selectById(1L)).thenReturn(SysFile.builder()
                .id(1L)
                .uploadUserId(10L)
                .build());
        FileService service = fileService(fileMapper, mock(SysRecycleBinMapper.class), mock(FileStorageService.class));

        assertThatThrownBy(() -> service.requireEdit(1L, 1L))
                .isInstanceOf(cn.datong.standard.common.BusinessException.class)
                .hasMessage("只有文件所有者可以改动删除该文件");
    }

    @Test
    void ownerCanReplaceFileAndOldObjectIsRemoved() {
        SysFileMapper fileMapper = mock(SysFileMapper.class);
        FileStorageService storageService = mock(FileStorageService.class);
        MultipartFile multipartFile = mock(MultipartFile.class);
        SysFile file = SysFile.builder()
                .id(1L)
                .fileName("旧文件.pdf")
                .originalFileName("旧文件.pdf")
                .extension("pdf")
                .mimeType("application/pdf")
                .fileSize(10L)
                .storageBucket("bucket")
                .storagePath("docs/old.pdf")
                .uploadUserId(10L)
                .versionNo(1)
                .build();
        when(fileMapper.selectById(1L)).thenReturn(file);
        when(multipartFile.getOriginalFilename()).thenReturn("新文件.docx");
        when(storageService.upload(any(), any())).thenReturn(new StoredObject("bucket", "docs/new.docx", 20L,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        FileService service = fileService(fileMapper, mock(SysRecycleBinMapper.class), storageService);

        SysFile result = service.replace(10L, true, 1L, multipartFile, null);

        assertThat(result.getFileName()).isEqualTo("新文件.docx");
        assertThat(result.getOriginalFileName()).isEqualTo("新文件.docx");
        assertThat(result.getExtension()).isEqualTo("docx");
        assertThat(result.getMimeType()).isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertThat(result.getFileSize()).isEqualTo(20L);
        assertThat(result.getStoragePath()).isEqualTo("docs/new.docx");
        assertThat(result.getVersionNo()).isEqualTo(2);
        assertThat(result.getLastEditTime()).isNotNull();
        verify(fileMapper).updateById(file);
        verify(storageService).remove("bucket", "docs/old.pdf");
    }

    @Test
    void nonOwnerCannotReplaceFileEvenWithSuperAdminFlag() {
        SysFileMapper fileMapper = mock(SysFileMapper.class);
        when(fileMapper.selectById(1L)).thenReturn(SysFile.builder()
                .id(1L)
                .uploadUserId(10L)
                .build());
        FileService service = fileService(fileMapper, mock(SysRecycleBinMapper.class), mock(FileStorageService.class));

        assertThatThrownBy(() -> service.replace(1L, true, 1L, mock(MultipartFile.class), null))
                .isInstanceOf(cn.datong.standard.common.BusinessException.class)
                .hasMessage("只有文件所有者可以改动删除该文件");
    }

    private FileService fileService(SysFileMapper fileMapper, SysRecycleBinMapper recycleBinMapper,
                                    FileStorageService storageService) {
        return new FileService(
                fileMapper,
                mock(SysFilePermissionMapper.class),
                recycleBinMapper,
                storageService,
                mock(FileAccessService.class),
                mock(PermissionService.class),
                mock(OperationLogService.class)
        );
    }
}
