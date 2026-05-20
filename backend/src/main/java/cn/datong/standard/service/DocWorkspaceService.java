package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.dto.DeptNavigationItem;
import cn.datong.standard.entity.SysDept;
import cn.datong.standard.entity.SysDocAttachment;
import cn.datong.standard.entity.SysDocCategory;
import cn.datong.standard.entity.SysDocItem;
import cn.datong.standard.entity.SysDocSubmission;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.mapper.SysDeptMapper;
import cn.datong.standard.mapper.SysDocAttachmentMapper;
import cn.datong.standard.mapper.SysDocCategoryMapper;
import cn.datong.standard.mapper.SysDocItemMapper;
import cn.datong.standard.mapper.SysDocSubmissionMapper;
import cn.datong.standard.mapper.SysUserMapper;
import cn.datong.standard.storage.FileStorageService;
import cn.datong.standard.storage.StoredObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocWorkspaceService {
    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;
    private final SysDocCategoryMapper categoryMapper;
    private final SysDocItemMapper itemMapper;
    private final SysDocSubmissionMapper submissionMapper;
    private final SysDocAttachmentMapper attachmentMapper;
    private final FileStorageService storageService;

    public List<DeptNavigationItem> sections() {
        return sectionDepts().stream()
                .map(dept -> new DeptNavigationItem(
                        dept.getId(),
                        dept.getParentId(),
                        dept.getDeptName(),
                        dept.getDeptCode(),
                        dept.getSortOrder(),
                        dept.getStatus(),
                        List.of()))
                .toList();
    }

    public List<SysDocCategory> categories(Long sectionDeptId) {
        requireSection(sectionDeptId);
        return categoryMapper.selectList(new LambdaQueryWrapper<SysDocCategory>()
                .eq(SysDocCategory::getSectionDeptId, sectionDeptId)
                .eq(SysDocCategory::getDeleted, 0)
                .orderByAsc(SysDocCategory::getSortOrder)
                .orderByAsc(SysDocCategory::getId));
    }

    public SysDocCategory createCategory(Long userId, Long userDeptId, boolean superAdmin, SysDocCategory request) {
        requireManageSection(userDeptId, superAdmin, request.getSectionDeptId());
        SysDocCategory category = new SysDocCategory();
        category.setSectionDeptId(request.getSectionDeptId());
        category.setCategoryName(requiredText(request.getCategoryName(), "请输入二级菜单名称"));
        category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        category.setCreatedBy(userId);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        category.setDeleted(0);
        categoryMapper.insert(category);
        return category;
    }

    public SysDocCategory updateCategory(Long userDeptId, boolean superAdmin, Long id, SysDocCategory request) {
        SysDocCategory existing = requireCategory(id);
        requireManageSection(userDeptId, superAdmin, existing.getSectionDeptId());
        existing.setCategoryName(requiredText(request.getCategoryName(), "请输入二级菜单名称"));
        existing.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        existing.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(existing);
        return existing;
    }

    public void deleteCategory(Long userDeptId, boolean superAdmin, Long id) {
        SysDocCategory existing = requireCategory(id);
        requireManageSection(userDeptId, superAdmin, existing.getSectionDeptId());
        categoryMapper.deleteById(id);
    }

    public List<SysDocItem> items(Long categoryId) {
        requireCategory(categoryId);
        List<SysDocItem> items = itemMapper.selectList(new LambdaQueryWrapper<SysDocItem>()
                .eq(SysDocItem::getCategoryId, categoryId)
                .eq(SysDocItem::getDeleted, 0)
                .orderByAsc(SysDocItem::getSortOrder)
                .orderByAsc(SysDocItem::getId));
        fillItemCounts(items);
        return items;
    }

    public SysDocItem item(Long id) {
        SysDocItem item = requireItem(id);
        fillItemCounts(List.of(item));
        fillItemInfo(List.of(item));
        return item;
    }

    public SysDocItem createItem(Long userId, Long userDeptId, boolean superAdmin, SysDocItem request) {
        SysDocCategory category = requireCategory(request.getCategoryId());
        requireManageSection(userDeptId, superAdmin, category.getSectionDeptId());
        SysDocItem item = new SysDocItem();
        item.setCategoryId(category.getId());
        applyItemRequest(item, request);
        item.setCreatedBy(userId);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        item.setDeleted(0);
        itemMapper.insert(item);
        return item;
    }

    public SysDocItem updateItem(Long userDeptId, boolean superAdmin, Long id, SysDocItem request) {
        SysDocItem item = requireItem(id);
        SysDocCategory category = requireCategory(item.getCategoryId());
        requireManageSection(userDeptId, superAdmin, category.getSectionDeptId());
        applyItemRequest(item, request);
        item.setUpdatedAt(LocalDateTime.now());
        itemMapper.updateById(item);
        return item;
    }

    public void deleteItem(Long userDeptId, boolean superAdmin, Long id) {
        SysDocItem item = requireItem(id);
        SysDocCategory category = requireCategory(item.getCategoryId());
        requireManageSection(userDeptId, superAdmin, category.getSectionDeptId());
        itemMapper.deleteById(id);
    }

    @Transactional
    public SysDocSubmission submit(Long userId, Long userDeptId, Long itemId, String valuesJson, List<MultipartFile> files) {
        SysDocItem item = requireItem(itemId);
        SysDept workshop = requireWorkshop(userDeptId);
        SysDocCategory category = requireCategory(item.getCategoryId());
        List<MultipartFile> uploadFiles = files == null ? List.of() : files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
        if (item.getAttachmentEnabled() == null || item.getAttachmentEnabled() == 0) {
            throw new BusinessException("该文件未开启附件上传");
        }
        if (uploadFiles.isEmpty()) {
            throw new BusinessException("请上传附件");
        }

        SysDocSubmission submission = new SysDocSubmission();
        submission.setItemId(item.getId());
        submission.setCategoryId(category.getId());
        submission.setSectionDeptId(category.getSectionDeptId());
        submission.setWorkshopDeptId(workshop.getId());
        submission.setUploadUserId(userId);
        submission.setSubmittedAt(LocalDateTime.now());
        submissionMapper.insert(submission);

        for (MultipartFile file : uploadFiles) {
            saveAttachment(userId, submission.getId(), file);
        }
        return detail(userId, userDeptId, false, submission.getId());
    }

    public List<SysDocSubmission> submissions(Long userId, Long userDeptId, boolean superAdmin, Long categoryId) {
        SysDocCategory category = requireCategory(categoryId);
        boolean sectionManager = canManageSection(userDeptId, superAdmin, category.getSectionDeptId());
        boolean workshopUser = isWorkshop(userDeptId);
        if (!sectionManager && !workshopUser && !superAdmin) {
            throw new BusinessException(403, "无权查看上传记录");
        }
        LambdaQueryWrapper<SysDocSubmission> wrapper = new LambdaQueryWrapper<SysDocSubmission>()
                .eq(SysDocSubmission::getCategoryId, categoryId)
                .orderByDesc(SysDocSubmission::getSubmittedAt);
        if (!superAdmin && !sectionManager) {
            wrapper.eq(SysDocSubmission::getWorkshopDeptId, userDeptId);
        }
        List<SysDocSubmission> result = submissionMapper.selectList(wrapper);
        fillSubmissionInfo(result);
        return result;
    }

    public List<SysDocSubmission> itemSubmissions(Long userId, Long userDeptId, boolean superAdmin, Long itemId) {
        SysDocItem item = requireItem(itemId);
        SysDocCategory category = requireCategory(item.getCategoryId());
        boolean sectionManager = canManageSection(userDeptId, superAdmin, category.getSectionDeptId());
        boolean workshopUser = isWorkshop(userDeptId);
        if (!sectionManager && !workshopUser && !superAdmin) {
            throw new BusinessException(403, "无权查看上传记录");
        }
        LambdaQueryWrapper<SysDocSubmission> wrapper = new LambdaQueryWrapper<SysDocSubmission>()
                .eq(SysDocSubmission::getItemId, itemId)
                .orderByDesc(SysDocSubmission::getSubmittedAt);
        if (!superAdmin && !sectionManager) {
            wrapper.eq(SysDocSubmission::getWorkshopDeptId, userDeptId);
        }
        List<SysDocSubmission> result = submissionMapper.selectList(wrapper);
        fillSubmissionInfo(result);
        return result;
    }

    public SysDocSubmission detail(Long userId, Long userDeptId, boolean superAdmin, Long submissionId) {
        SysDocSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException("上传记录不存在");
        }
        requireSubmissionVisible(userDeptId, superAdmin, submission);
        fillSubmissionInfo(List.of(submission));
        submission.setAttachments(attachments(submissionId));
        return submission;
    }

    public SysDocAttachment requireAttachment(Long userDeptId, boolean superAdmin, Long attachmentId) {
        SysDocAttachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException("附件不存在");
        }
        SysDocSubmission submission = submissionMapper.selectById(attachment.getSubmissionId());
        if (submission == null) {
            throw new BusinessException("上传记录不存在");
        }
        requireSubmissionVisible(userDeptId, superAdmin, submission);
        return attachment;
    }

    private void applyItemRequest(SysDocItem item, SysDocItem request) {
        item.setItemName(requiredText(request.getItemName(), "请输入文件名称"));
        item.setContentHtml(request.getContentHtml() == null ? "" : request.getContentHtml());
        item.setAttachmentEnabled(request.getAttachmentEnabled() != null && request.getAttachmentEnabled() == 1 ? 1 : 0);
        item.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
    }

    private void saveAttachment(Long userId, Long submissionId, MultipartFile file) {
        String original = file.getOriginalFilename() == null ? "未命名文件" : file.getOriginalFilename();
        String extension = extension(original);
        String objectName = "doc-submissions/" + LocalDate.now() + "/" + UUID.randomUUID()
                + (extension.isBlank() ? "" : "." + extension);
        StoredObject stored = storageService.upload(file, objectName);
        SysDocAttachment attachment = new SysDocAttachment();
        attachment.setSubmissionId(submissionId);
        attachment.setOriginalFileName(original);
        attachment.setExtension(extension);
        attachment.setMimeType(stored.contentType());
        attachment.setFileSize(stored.size());
        attachment.setStorageBucket(stored.bucket());
        attachment.setStoragePath(stored.objectName());
        attachment.setUploadedBy(userId);
        attachment.setCreatedAt(LocalDateTime.now());
        attachmentMapper.insert(attachment);
    }

    private List<SysDocAttachment> attachments(Long submissionId) {
        return attachmentMapper.selectList(new LambdaQueryWrapper<SysDocAttachment>()
                .eq(SysDocAttachment::getSubmissionId, submissionId)
                .orderByAsc(SysDocAttachment::getId));
    }

    private void fillItemCounts(List<SysDocItem> items) {
        for (SysDocItem item : items) {
            item.setSubmissionCount(Math.toIntExact(submissionMapper.selectCount(new LambdaQueryWrapper<SysDocSubmission>()
                    .eq(SysDocSubmission::getItemId, item.getId()))));
        }
    }

    private void fillItemInfo(List<SysDocItem> items) {
        if (items.isEmpty()) {
            return;
        }
        Map<Long, SysDocCategory> categoryMap = categoryMapper.selectList(new LambdaQueryWrapper<SysDocCategory>()
                        .eq(SysDocCategory::getDeleted, 0))
                .stream().collect(Collectors.toMap(SysDocCategory::getId, Function.identity(), (a, b) -> a));
        Map<Long, SysDept> deptMap = deptMapper.selectList(new LambdaQueryWrapper<SysDept>().eq(SysDept::getDeleted, 0))
                .stream().collect(Collectors.toMap(SysDept::getId, Function.identity(), (a, b) -> a));
        for (SysDocItem item : items) {
            SysDocCategory category = categoryMap.get(item.getCategoryId());
            if (category == null) {
                continue;
            }
            item.setCategoryName(category.getCategoryName());
            item.setSectionDeptId(category.getSectionDeptId());
            item.setSectionDeptName(name(deptMap.get(category.getSectionDeptId())));
        }
    }

    private void fillSubmissionInfo(List<SysDocSubmission> submissions) {
        if (submissions.isEmpty()) {
            return;
        }
        Map<Long, SysDept> deptMap = deptMapper.selectList(new LambdaQueryWrapper<SysDept>().eq(SysDept::getDeleted, 0))
                .stream().collect(Collectors.toMap(SysDept::getId, Function.identity(), (a, b) -> a));
        Map<Long, SysUser> userMap = userMapper.selectList(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeleted, 0))
                .stream().collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));
        Map<Long, SysDocCategory> categoryMap = categoryMapper.selectList(new LambdaQueryWrapper<SysDocCategory>()
                        .eq(SysDocCategory::getDeleted, 0))
                .stream().collect(Collectors.toMap(SysDocCategory::getId, Function.identity(), (a, b) -> a));
        Map<Long, SysDocItem> itemMap = itemMapper.selectList(new LambdaQueryWrapper<SysDocItem>()
                        .eq(SysDocItem::getDeleted, 0))
                .stream().collect(Collectors.toMap(SysDocItem::getId, Function.identity(), (a, b) -> a));
        for (SysDocSubmission submission : submissions) {
            submission.setSectionDeptName(name(deptMap.get(submission.getSectionDeptId())));
            submission.setWorkshopDeptName(name(deptMap.get(submission.getWorkshopDeptId())));
            submission.setCategoryName(categoryMap.get(submission.getCategoryId()) == null ? null : categoryMap.get(submission.getCategoryId()).getCategoryName());
            submission.setItemName(itemMap.get(submission.getItemId()) == null ? null : itemMap.get(submission.getItemId()).getItemName());
            SysUser user = userMap.get(submission.getUploadUserId());
            submission.setUploadUserName(user == null ? null : (user.getRealName() == null ? user.getUsername() : user.getRealName()));
            submission.setAttachmentCount(Math.toIntExact(attachmentMapper.selectCount(new LambdaQueryWrapper<SysDocAttachment>()
                    .eq(SysDocAttachment::getSubmissionId, submission.getId()))));
        }
    }

    private void requireSubmissionVisible(Long userDeptId, boolean superAdmin, SysDocSubmission submission) {
        if (superAdmin || Objects.equals(userDeptId, submission.getSectionDeptId()) || Objects.equals(userDeptId, submission.getWorkshopDeptId())) {
            return;
        }
        throw new BusinessException(403, "无权查看上传记录");
    }

    private boolean canManageSection(Long userDeptId, boolean superAdmin, Long sectionDeptId) {
        return superAdmin || Objects.equals(userDeptId, sectionDeptId);
    }

    private void requireManageSection(Long userDeptId, boolean superAdmin, Long sectionDeptId) {
        requireSection(sectionDeptId);
        if (!canManageSection(userDeptId, superAdmin, sectionDeptId)) {
            throw new BusinessException(403, "只能维护本科室资料");
        }
    }

    private SysDept requireSection(Long sectionDeptId) {
        SysDept dept = deptMapper.selectById(sectionDeptId);
        if (dept == null || dept.getDeleted() != null && dept.getDeleted() == 1 || !isSection(dept)) {
            throw new BusinessException("科室不存在");
        }
        return dept;
    }

    private SysDept requireWorkshop(Long workshopDeptId) {
        SysDept dept = deptMapper.selectById(workshopDeptId);
        if (dept == null || dept.getDeleted() != null && dept.getDeleted() == 1 || !isWorkshop(dept)) {
            throw new BusinessException(403, "只有车间用户可以提交资料");
        }
        return dept;
    }

    private SysDocCategory requireCategory(Long id) {
        SysDocCategory category = categoryMapper.selectById(id);
        if (category == null || category.getDeleted() != null && category.getDeleted() == 1) {
            throw new BusinessException("二级菜单不存在");
        }
        return category;
    }

    private SysDocItem requireItem(Long id) {
        SysDocItem item = itemMapper.selectById(id);
        if (item == null || item.getDeleted() != null && item.getDeleted() == 1) {
            throw new BusinessException("资料入口不存在");
        }
        return item;
    }

    private List<SysDept> sectionDepts() {
        return deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                        .eq(SysDept::getDeleted, 0)
                        .eq(SysDept::getStatus, "ENABLED"))
                .stream()
                .filter(this::isSection)
                .sorted(Comparator.comparing(SysDept::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SysDept::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private boolean isSection(Long deptId) {
        SysDept dept = deptId == null ? null : deptMapper.selectById(deptId);
        return dept != null && isSection(dept);
    }

    private boolean isWorkshop(Long deptId) {
        SysDept dept = deptId == null ? null : deptMapper.selectById(deptId);
        return dept != null && isWorkshop(dept);
    }

    private boolean isSection(SysDept dept) {
        if ("SECTION".equalsIgnoreCase(dept.getDeptType())) {
            return true;
        }
        return dept.getParentId() != null && dept.getParentId() > 0 && !"机关".equals(dept.getDeptName());
    }

    private boolean isWorkshop(SysDept dept) {
        if ("WORKSHOP".equalsIgnoreCase(dept.getDeptType())) {
            return true;
        }
        return dept.getDeptName() != null && dept.getDeptName().contains("车间");
    }

    private String requiredText(String value, String message) {
        if (value == null || value.trim().isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String extension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx >= 0 && idx < filename.length() - 1 ? filename.substring(idx + 1).toLowerCase(Locale.ROOT) : "";
    }

    private String name(SysDept dept) {
        return dept == null ? null : dept.getDeptName();
    }
}
