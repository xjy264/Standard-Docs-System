package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.dto.FileSearchRequest;
import cn.datong.standard.entity.SysDept;
import cn.datong.standard.entity.SysFile;
import cn.datong.standard.entity.SysFilePermissionRow;
import cn.datong.standard.entity.SysRecycleBin;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.enums.TargetType;
import cn.datong.standard.enums.VisibilityScope;
import cn.datong.standard.mapper.SysDeptMapper;
import cn.datong.standard.mapper.SysFileMapper;
import cn.datong.standard.mapper.SysFilePermissionMapper;
import cn.datong.standard.mapper.SysRecycleBinMapper;
import cn.datong.standard.mapper.SysUserMapper;
import cn.datong.standard.storage.FileStorageService;
import cn.datong.standard.storage.StoredObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {
    private final SysFileMapper fileMapper;
    private final SysFilePermissionMapper filePermissionMapper;
    private final SysRecycleBinMapper recycleBinMapper;
    private final FileStorageService storageService;
    private final FileAccessService fileAccessService;
    private final PermissionService permissionService;
    private final OperationLogService logService;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;

    public SysFile upload(Long userId, Long deptId, boolean superAdmin, MultipartFile multipartFile, Long folderId,
                          VisibilityScope visibilityScope, List<Long> userIds, List<Long> deptIds,
                          HttpServletRequest request) {
        permissionService.require(userId, superAdmin, "file:upload");
        String original = multipartFile.getOriginalFilename() == null ? "未命名文件" : multipartFile.getOriginalFilename();
        String extension = extension(original);
        String objectName = LocalDateTime.now().toLocalDate() + "/" + UUID.randomUUID() + "." + extension;
        StoredObject storedObject = storageService.upload(multipartFile, objectName);

        SysFile file = new SysFile();
        file.setFileName(original);
        file.setOriginalFileName(original);
        file.setExtension(extension);
        file.setMimeType(storedObject.contentType());
        file.setFileSize(storedObject.size());
        file.setStorageBucket(storedObject.bucket());
        file.setStoragePath(storedObject.objectName());
        file.setFolderId(folderId);
        file.setDeptId(deptId);
        file.setUploadUserId(userId);
        file.setVisibilityScope(visibilityScope == null ? VisibilityScope.DEPT : visibilityScope);
        file.setStatus("NORMAL");
        file.setVersionNo(1);
        file.setDownloadCount(0L);
        file.setPreviewCount(0L);
        file.setDeleted(0);
        file.setCreatedAt(LocalDateTime.now());
        file.setUpdatedAt(LocalDateTime.now());
        fileMapper.insert(file);

        logService.operation(userId, "上传文件", "FILE", file.getId(), "SUCCESS", null, request);
        return file;
    }

    public List<SysFile> search(Long userId, Long deptId, boolean superAdmin, FileSearchRequest request) {
        List<String> extensions = searchExtensions(request.extension());
        LambdaQueryWrapper<SysFile> wrapper = new LambdaQueryWrapper<SysFile>()
                .eq(SysFile::getDeleted, 0)
                .like(request.keyword() != null && !request.keyword().isBlank(), SysFile::getFileName, request.keyword())
                .in(!extensions.isEmpty(), SysFile::getExtension, extensions)
                .eq(request.deptId() != null, SysFile::getDeptId, request.deptId())
                .eq(request.folderId() != null, SysFile::getFolderId, request.folderId())
                .eq(request.visibilityScope() != null, SysFile::getVisibilityScope, request.visibilityScope())
                .eq(Boolean.TRUE.equals(request.mine()), SysFile::getUploadUserId, userId)
                .ge(request.uploadStart() != null, SysFile::getCreatedAt, request.uploadStart() == null ? null : request.uploadStart().atStartOfDay())
                .lt(request.uploadEnd() != null, SysFile::getCreatedAt, request.uploadEnd() == null ? null : request.uploadEnd().plusDays(1).atStartOfDay())
                .orderByDesc(SysFile::getCreatedAt);
        List<SysFile> files = fileMapper.selectList(wrapper).stream()
                .filter(file -> !Boolean.TRUE.equals(request.mine()) || userId.equals(file.getUploadUserId()))
                .filter(file -> fileAccessService.canAccess(userId, deptId, superAdmin, file.getId()))
                .toList();
        fillOwnerInfo(files);
        return files;
    }

    public SysFile detail(Long userId, Long deptId, boolean superAdmin, Long fileId) {
        fileAccessService.requireAccess(userId, deptId, superAdmin, fileId);
        SysFile file = fileMapper.selectById(fileId);
        file.setLastViewTime(LocalDateTime.now());
        file.setPreviewCount(file.getPreviewCount() == null ? 1 : file.getPreviewCount() + 1);
        fileMapper.updateById(file);
        return file;
    }

    public SysFile requireDownload(Long userId, Long deptId, boolean superAdmin, Long fileId, HttpServletRequest request) {
        fileAccessService.requireAccess(userId, deptId, superAdmin, fileId);
        SysFile file = fileMapper.selectById(fileId);
        file.setDownloadCount(file.getDownloadCount() == null ? 1 : file.getDownloadCount() + 1);
        fileMapper.updateById(file);
        logService.operation(userId, "下载文件", "FILE", fileId, "SUCCESS", null, request);
        return file;
    }

    public void softDelete(Long userId, Long deptId, boolean superAdmin, Long fileId, HttpServletRequest request) {
        SysFile file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }
        requireOwner(userId, file);
        file.setDeleted(1);
        file.setDeletedBy(userId);
        file.setDeletedAt(LocalDateTime.now());
        fileMapper.updateById(file);

        SysRecycleBin recycle = new SysRecycleBin();
        recycle.setFileId(fileId);
        recycle.setOriginalPath(file.getStoragePath());
        recycle.setDeletedBy(userId);
        recycle.setDeletedAt(LocalDateTime.now());
        recycle.setDeptId(file.getDeptId());
        recycle.setFileSize(file.getFileSize());
        recycle.setStatus("ACTIVE");
        recycleBinMapper.insert(recycle);
        logService.operation(userId, "删除文件", "FILE", fileId, "SUCCESS", null, request);
    }

    public void restore(Long userId, boolean superAdmin, Long fileId, HttpServletRequest request) {
        SysFile file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }
        requireOwner(userId, file);
        file.setDeleted(0);
        file.setDeletedBy(null);
        file.setDeletedAt(null);
        fileMapper.updateById(file);
        logService.operation(userId, "恢复文件", "FILE", fileId, "SUCCESS", null, request);
    }

    public void remove(Long userId, boolean superAdmin, Long fileId, HttpServletRequest request) {
        SysFile file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }
        requireOwner(userId, file);
        storageService.remove(file.getStorageBucket(), file.getStoragePath());
        fileMapper.deleteById(fileId);
        logService.operation(userId, "彻底删除文件", "FILE", fileId, "SUCCESS", null, request);
    }

    public SysFile requireEdit(Long userId, Long fileId) {
        SysFile file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }
        requireOwner(userId, file);
        return file;
    }

    public SysFile replace(Long userId, boolean superAdmin, Long fileId, MultipartFile multipartFile, HttpServletRequest request) {
        SysFile file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }
        requireOwner(userId, file);
        String oldBucket = file.getStorageBucket();
        String oldPath = file.getStoragePath();
        String original = multipartFile.getOriginalFilename() == null ? "未命名文件" : multipartFile.getOriginalFilename();
        String extension = extension(original);
        String objectName = LocalDateTime.now().toLocalDate() + "/" + UUID.randomUUID() + "." + extension;
        StoredObject storedObject = storageService.upload(multipartFile, objectName);

        file.setFileName(original);
        file.setOriginalFileName(original);
        file.setExtension(extension);
        file.setMimeType(storedObject.contentType());
        file.setFileSize(storedObject.size());
        file.setStorageBucket(storedObject.bucket());
        file.setStoragePath(storedObject.objectName());
        file.setVersionNo(file.getVersionNo() == null ? 1 : file.getVersionNo() + 1);
        file.setLastEditTime(LocalDateTime.now());
        file.setUpdatedAt(LocalDateTime.now());
        fileMapper.updateById(file);

        removeOldObject(oldBucket, oldPath);
        logService.operation(userId, "替换文件", "FILE", fileId, "SUCCESS", null, request);
        return file;
    }

    private void requireOwner(Long userId, SysFile file) {
        if (userId == null || !userId.equals(file.getUploadUserId())) {
            throw new BusinessException(403, "只有文件所有者可以改动删除该文件");
        }
    }

    private void removeOldObject(String bucket, String objectName) {
        if (bucket == null || objectName == null) {
            return;
        }
        try {
            storageService.remove(bucket, objectName);
        } catch (RuntimeException ex) {
            log.warn("替换文件后清理旧文件失败：bucket={}, object={}", bucket, objectName, ex);
        }
    }

    private void fillOwnerInfo(List<SysFile> files) {
        Set<Long> userIds = files.stream()
                .map(SysFile::getUploadUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        List<SysUser> owners = userMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, userIds));
        Map<Long, SysUser> ownerMap = owners.stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Set<Long> deptIds = files.stream()
                .map(file -> {
                    SysUser owner = ownerMap.get(file.getUploadUserId());
                    return owner != null && owner.getDeptId() != null ? owner.getDeptId() : file.getDeptId();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SysDept> deptMap = deptIds.isEmpty()
                ? Map.of()
                : deptMapper.selectList(new LambdaQueryWrapper<SysDept>().in(SysDept::getId, deptIds)).stream()
                .collect(Collectors.toMap(SysDept::getId, Function.identity()));
        for (SysFile file : files) {
            SysUser owner = ownerMap.get(file.getUploadUserId());
            if (owner != null) {
                file.setOwnerName(owner.getRealName() == null || owner.getRealName().isBlank()
                        ? owner.getUsername()
                        : owner.getRealName());
            }
            Long ownerDeptId = owner != null && owner.getDeptId() != null ? owner.getDeptId() : file.getDeptId();
            SysDept dept = ownerDeptId == null ? null : deptMap.get(ownerDeptId);
            if (dept != null) {
                file.setOwnerDeptName(dept.getDeptName());
            }
        }
    }

    private void grant(Long fileId, List<Long> ids, TargetType targetType, Long createdBy) {
        if (ids == null) {
            return;
        }
        for (Long id : ids) {
            SysFilePermissionRow row = new SysFilePermissionRow();
            row.setFileId(fileId);
            row.setTargetType(targetType);
            row.setTargetId(id);
            row.setAccessType("ACCESS");
            row.setCreatedBy(createdBy);
            row.setCreatedAt(LocalDateTime.now());
            filePermissionMapper.insert(row);
        }
    }

    private String extension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "bin";
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    static List<String> searchExtensions(String extension) {
        if (extension == null || extension.isBlank()) {
            return List.of();
        }
        return switch (extension.trim().toLowerCase(Locale.ROOT)) {
            case "doc", "docx" -> List.of("doc", "docx");
            case "xls", "xlsx" -> List.of("xls", "xlsx");
            case "ppt", "pptx" -> List.of("ppt", "pptx");
            case "dwg", "dxf" -> List.of("dwg", "dxf");
            case "jpg", "jpeg", "png" -> List.of("jpg", "jpeg", "png");
            case "zip", "rar", "7z" -> List.of("zip", "rar", "7z");
            default -> List.of(extension.trim().toLowerCase(Locale.ROOT));
        };
    }
}
