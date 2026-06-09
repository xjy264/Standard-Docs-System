package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.dto.DeptNavigationItem;
import cn.datong.standard.dto.DocNodeRequest;
import cn.datong.standard.dto.DocUploadRequirementRequest;
import cn.datong.standard.entity.SysDept;
import cn.datong.standard.entity.SysDocAttachment;
import cn.datong.standard.entity.SysDocCategory;
import cn.datong.standard.entity.SysDocItem;
import cn.datong.standard.entity.SysDocItemAttachment;
import cn.datong.standard.entity.SysDocNode;
import cn.datong.standard.entity.SysDocSubmission;
import cn.datong.standard.entity.SysDocUploadRequirement;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.mapper.SysDeptMapper;
import cn.datong.standard.mapper.SysDocAttachmentMapper;
import cn.datong.standard.mapper.SysDocCategoryMapper;
import cn.datong.standard.mapper.SysDocItemMapper;
import cn.datong.standard.mapper.SysDocItemAttachmentMapper;
import cn.datong.standard.mapper.SysDocNodeMapper;
import cn.datong.standard.mapper.SysDocSubmissionMapper;
import cn.datong.standard.mapper.SysDocUploadRequirementMapper;
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
import java.util.ArrayList;
import java.util.Comparator;
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
public class DocWorkspaceService {
    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;
    private final SysDocCategoryMapper categoryMapper;
    private final SysDocItemMapper itemMapper;
    private final SysDocNodeMapper nodeMapper;
    private final SysDocSubmissionMapper submissionMapper;
    private final SysDocAttachmentMapper attachmentMapper;
    private final SysDocUploadRequirementMapper requirementMapper;
    private final SysDocItemAttachmentMapper itemAttachmentMapper;
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

    public List<SysDocNode> documentTree(Long sectionDeptId) {
        return documentTree(null, null, false, sectionDeptId, null);
    }

    public List<SysDocNode> documentTree(Long userId, Long userDeptId, boolean superAdmin, Long sectionDeptId, String businessType) {
        requireSection(sectionDeptId);
        String normalizedBusinessType = normalizeBusinessTypeOrNull(businessType);
        List<SysDocNode> nodes = nodeMapper.selectList(new LambdaQueryWrapper<SysDocNode>()
                .eq(SysDocNode::getSectionDeptId, sectionDeptId)
                .eq(SysDocNode::getDeleted, 0)
                .orderByAsc(SysDocNode::getSortOrder)
                .orderByAsc(SysDocNode::getId));
        fillNodeItemInfo(nodes);
        nodes = nodes.stream()
                .filter(node -> !"FILE".equalsIgnoreCase(node.getNodeType())
                        || normalizedBusinessType == null
                        || normalizedBusinessType.equalsIgnoreCase(node.getBusinessType()))
                .toList();
        Map<Long, SysDocNode> nodeMap = nodes.stream()
                .peek(node -> node.setChildren(new ArrayList<>()))
                .collect(Collectors.toMap(SysDocNode::getId, Function.identity(), (a, b) -> a));
        List<SysDocNode> roots = new ArrayList<>();
        for (SysDocNode node : nodes) {
            if (node.getParentId() == null || !nodeMap.containsKey(node.getParentId())) {
                roots.add(node);
            } else {
                nodeMap.get(node.getParentId()).getChildren().add(node);
            }
        }
        if ("UPLOAD".equals(normalizedBusinessType)) {
            Set<Long> completedItemIds = completedUploadItemIds(userId, userDeptId, superAdmin, sectionDeptId);
            roots.forEach(root -> fillUploadProgress(root, completedItemIds));
        }
        return roots;
    }

    public SysDocNode createFolderNode(Long userId, Long userDeptId, boolean superAdmin, DocNodeRequest request) {
        NodePlacement placement = resolvePlacement(userDeptId, superAdmin, request.sectionDeptId(), request.parentId());
        SysDocNode node = new SysDocNode();
        node.setSectionDeptId(placement.sectionDeptId());
        node.setParentId(request.parentId());
        node.setNodeType("FOLDER");
        node.setNodeName(requiredText(request.nodeName(), "请输入文件夹名称"));
        node.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        node.setLevel(placement.level());
        node.setCreatedBy(userId);
        node.setCreatedAt(LocalDateTime.now());
        node.setUpdatedAt(LocalDateTime.now());
        node.setDeleted(0);
        nodeMapper.insert(node);
        return node;
    }

    @Transactional
    public SysDocNode createFileNode(Long userId, Long userDeptId, boolean superAdmin, DocNodeRequest request) {
        NodePlacement placement = resolvePlacement(userDeptId, superAdmin, request.sectionDeptId(), request.parentId());
        String businessType = normalizeBusinessType(request.businessType());
        SysDocItem item = new SysDocItem();
        item.setSectionDeptId(placement.sectionDeptId());
        item.setItemName(requiredText(request.nodeName(), "请输入文件名称"));
        item.setBusinessType(businessType);
        item.setSubmitterMode("UPLOAD".equals(businessType) ? normalizeSubmitterMode(request.submitterMode()) : "SINGLE");
        item.setFileType(normalizeFileType(request.fileType()));
        item.setContentHtml(request.contentHtml() == null ? "" : request.contentHtml());
        item.setAttachmentEnabled("UPLOAD".equals(businessType) ? 1 : 0);
        item.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        item.setCreatedBy(userId);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        item.setDeleted(0);
        itemMapper.insert(item);

        SysDocNode node = new SysDocNode();
        node.setSectionDeptId(placement.sectionDeptId());
        node.setParentId(request.parentId());
        node.setNodeType("FILE");
        node.setNodeName(item.getItemName());
        node.setItemId(item.getId());
        node.setSortOrder(item.getSortOrder());
        node.setLevel(placement.level());
        node.setCreatedBy(userId);
        node.setCreatedAt(LocalDateTime.now());
        node.setUpdatedAt(LocalDateTime.now());
        node.setDeleted(0);
        nodeMapper.insert(node);
        if ("UPLOAD".equals(businessType)) {
            replaceRequirements(item.getId(), request.requirements());
        }
        return node;
    }

    @Transactional
    public SysDocNode updateNode(Long userDeptId, boolean superAdmin, Long id, DocNodeRequest request) {
        SysDocNode node = requireNode(id);
        requireManageSection(userDeptId, superAdmin, node.getSectionDeptId());
        node.setNodeName(requiredText(request.nodeName(), "请输入名称"));
        node.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        node.setUpdatedAt(LocalDateTime.now());
        nodeMapper.updateById(node);
        if ("FILE".equalsIgnoreCase(node.getNodeType()) && node.getItemId() != null) {
            SysDocItem item = requireItem(node.getItemId());
            String businessType = normalizeBusinessType(request.businessType() == null ? item.getBusinessType() : request.businessType());
            item.setItemName(node.getNodeName());
            item.setSortOrder(node.getSortOrder());
            item.setBusinessType(businessType);
            item.setSubmitterMode("UPLOAD".equals(businessType) ? normalizeSubmitterMode(request.submitterMode()) : "SINGLE");
            item.setFileType(normalizeFileType(request.fileType()));
            if (request.contentHtml() != null) {
                item.setContentHtml(request.contentHtml());
            }
            item.setAttachmentEnabled("UPLOAD".equals(businessType) ? 1 : 0);
            item.setUpdatedAt(LocalDateTime.now());
            itemMapper.updateById(item);
            if ("UPLOAD".equals(businessType)) {
                replaceRequirements(item.getId(), request.requirements());
            } else {
                deleteRequirements(item.getId());
            }
        }
        return node;
    }

    @Transactional
    public void deleteNode(Long userDeptId, boolean superAdmin, Long id) {
        SysDocNode node = requireNode(id);
        requireManageSection(userDeptId, superAdmin, node.getSectionDeptId());
        if ("FOLDER".equalsIgnoreCase(node.getNodeType()) && hasUndeletedDescendant(node)) {
            throw new BusinessException("文件夹下存在未删除内容，无法删除");
        }
        if ("FILE".equalsIgnoreCase(node.getNodeType()) && node.getItemId() != null) {
            itemMapper.deleteById(node.getItemId());
        }
        nodeMapper.deleteById(id);
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
        item.setRequirements(requirements(item.getId()));
        item.setIssuedAttachments(itemAttachments(item.getId()));
        return item;
    }

    @Transactional
    public SysDocItem createItem(Long userId, Long userDeptId, boolean superAdmin, SysDocItem request) {
        SysDocCategory category = requireCategory(request.getCategoryId());
        requireManageSection(userDeptId, superAdmin, category.getSectionDeptId());
        SysDocItem item = new SysDocItem();
        item.setCategoryId(category.getId());
        item.setSectionDeptId(category.getSectionDeptId());
        applyItemRequest(item, request);
        item.setCreatedBy(userId);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        item.setDeleted(0);
        itemMapper.insert(item);
        if ("UPLOAD".equals(item.getBusinessType())) {
            replaceRequirements(item.getId(), requirementRequests(request.getRequirements()));
        }
        return item;
    }

    @Transactional
    public SysDocItem updateItem(Long userDeptId, boolean superAdmin, Long id, SysDocItem request) {
        SysDocItem item = requireItem(id);
        requireManageSection(userDeptId, superAdmin, itemSectionDeptId(item));
        applyItemRequest(item, request);
        item.setUpdatedAt(LocalDateTime.now());
        itemMapper.updateById(item);
        if ("UPLOAD".equals(item.getBusinessType())) {
            replaceRequirements(item.getId(), requirementRequests(request.getRequirements()));
        } else {
            deleteRequirements(item.getId());
        }
        return item;
    }

    public void deleteItem(Long userDeptId, boolean superAdmin, Long id) {
        SysDocItem item = requireItem(id);
        requireManageSection(userDeptId, superAdmin, itemSectionDeptId(item));
        itemMapper.deleteById(id);
    }

    @Transactional
    public SysDocSubmission submit(Long userId, Long userDeptId, Long itemId, String valuesJson, List<MultipartFile> files) {
        return submit(userId, userDeptId, itemId, valuesJson, null, files);
    }

    @Transactional
    public SysDocSubmission submit(Long userId, Long userDeptId, Long itemId, String valuesJson, List<Long> requirementIds, List<MultipartFile> files) {
        SysDocItem item = requireItem(itemId);
        SysDocCategory category = item.getCategoryId() == null ? null : requireCategory(item.getCategoryId());
        Long sectionDeptId = itemSectionDeptId(item, category);
        SysDept submitterDept = submitterDept(userDeptId);
        List<MultipartFile> uploadFiles = files == null ? List.of() : files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
        if (!isUploadItem(item)) {
            throw new BusinessException("该文件不是上传任务");
        }
        if (uploadFiles.isEmpty()) {
            throw new BusinessException("请上传附件");
        }
        List<SysDocUploadRequirement> requirements = requirements(item.getId());
        List<Long> normalizedRequirementIds = normalizeSubmissionRequirementIds(requirements, requirementIds, uploadFiles);
        requireSubmissionOpen(userId, item);

        SysDocSubmission submission = new SysDocSubmission();
        submission.setItemId(item.getId());
        submission.setCategoryId(category == null ? null : category.getId());
        submission.setSectionDeptId(sectionDeptId);
        submission.setWorkshopDeptId(submitterDept != null && isWorkshop(submitterDept) ? submitterDept.getId() : null);
        submission.setSubmitterDeptId(submitterDept == null ? null : submitterDept.getId());
        submission.setUploadUserId(userId);
        submission.setSubmittedAt(LocalDateTime.now());
        submissionMapper.insert(submission);

        for (int i = 0; i < uploadFiles.size(); i++) {
            saveAttachment(userId, submission.getId(), normalizedRequirementIds.get(i), uploadFiles.get(i));
        }
        return detail(userId, userDeptId, false, submission.getId());
    }

    public List<SysDocSubmission> submissions(Long userId, Long userDeptId, boolean superAdmin, Long categoryId) {
        SysDocCategory category = requireCategory(categoryId);
        boolean sectionManager = canManageSection(userDeptId, superAdmin, category.getSectionDeptId());
        LambdaQueryWrapper<SysDocSubmission> wrapper = new LambdaQueryWrapper<SysDocSubmission>()
                .eq(SysDocSubmission::getCategoryId, categoryId)
                .orderByDesc(SysDocSubmission::getSubmittedAt);
        if (!superAdmin && !sectionManager) {
            if (userDeptId == null) {
                wrapper.eq(SysDocSubmission::getUploadUserId, userId);
            } else {
                wrapper.eq(SysDocSubmission::getSubmitterDeptId, userDeptId);
            }
        }
        List<SysDocSubmission> result = submissionMapper.selectList(wrapper);
        fillSubmissionInfo(result);
        return result;
    }

    public List<SysDocSubmission> itemSubmissions(Long userId, Long userDeptId, boolean superAdmin, Long itemId) {
        SysDocItem item = requireItem(itemId);
        boolean sectionManager = canManageSection(userDeptId, superAdmin, itemSectionDeptId(item));
        if (!superAdmin && !sectionManager) {
            throw new BusinessException(403, "只有科室用户可以查看全部上传记录");
        }
        LambdaQueryWrapper<SysDocSubmission> wrapper = new LambdaQueryWrapper<SysDocSubmission>()
                .eq(SysDocSubmission::getItemId, itemId)
                .orderByDesc(SysDocSubmission::getSubmittedAt);
        List<SysDocSubmission> result = submissionMapper.selectList(wrapper);
        fillSubmissionInfo(result);
        result.forEach(submission -> submission.setAttachments(attachments(submission.getId())));
        return result;
    }

    public SysDocSubmission mySubmission(Long userId, Long userDeptId, boolean superAdmin, Long itemId) {
        requireItem(itemId);
        SysDocSubmission submission = submissionMapper.selectOne(new LambdaQueryWrapper<SysDocSubmission>()
                .eq(SysDocSubmission::getItemId, itemId)
                .eq(SysDocSubmission::getUploadUserId, userId)
                .orderByDesc(SysDocSubmission::getSubmittedAt)
                .last("LIMIT 1"));
        if (submission == null) {
            return null;
        }
        requireSubmissionVisible(userId, userDeptId, superAdmin, submission);
        fillSubmissionInfo(List.of(submission));
        submission.setAttachments(attachments(submission.getId()));
        return submission;
    }

    public SysDocSubmission detail(Long userId, Long userDeptId, boolean superAdmin, Long submissionId) {
        SysDocSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException("上传记录不存在");
        }
        requireSubmissionVisible(userId, userDeptId, superAdmin, submission);
        fillSubmissionInfo(List.of(submission));
        submission.setAttachments(attachments(submissionId));
        return submission;
    }

    public SysDocAttachment requireAttachment(Long userId, Long userDeptId, boolean superAdmin, Long attachmentId) {
        SysDocAttachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException("附件不存在");
        }
        SysDocSubmission submission = submissionMapper.selectById(attachment.getSubmissionId());
        if (submission == null) {
            throw new BusinessException("上传记录不存在");
        }
        requireSubmissionVisible(userId, userDeptId, superAdmin, submission);
        return attachment;
    }

    @Transactional
    public List<SysDocItemAttachment> addItemAttachments(Long userId, Long userDeptId, boolean superAdmin, Long itemId, List<MultipartFile> files) {
        SysDocItem item = requireItem(itemId);
        requireManageSection(userDeptId, superAdmin, itemSectionDeptId(item));
        if (!"ISSUED".equals(normalizeBusinessType(item))) {
            throw new BusinessException("只有下达文件可以上传下达附件");
        }
        List<MultipartFile> uploadFiles = files == null ? List.of() : files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
        if (uploadFiles.isEmpty()) {
            throw new BusinessException("请上传附件");
        }
        for (MultipartFile file : uploadFiles) {
            saveItemAttachment(userId, item.getId(), file);
        }
        return itemAttachments(item.getId());
    }

    public SysDocItemAttachment requireItemAttachment(Long attachmentId) {
        SysDocItemAttachment attachment = itemAttachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException("附件不存在");
        }
        requireItem(attachment.getItemId());
        return attachment;
    }

    private void applyItemRequest(SysDocItem item, SysDocItem request) {
        item.setItemName(requiredText(request.getItemName(), "请输入文件名称"));
        String businessType = request.getBusinessType() == null
                ? (request.getAttachmentEnabled() != null && request.getAttachmentEnabled() == 1 ? "UPLOAD" : "ISSUED")
                : request.getBusinessType();
        item.setBusinessType(normalizeBusinessType(businessType));
        item.setSubmitterMode("UPLOAD".equals(item.getBusinessType()) ? normalizeSubmitterMode(request.getSubmitterMode()) : "SINGLE");
        item.setFileType(normalizeFileType(request.getFileType()));
        item.setContentHtml(request.getContentHtml() == null ? "" : request.getContentHtml());
        item.setAttachmentEnabled("UPLOAD".equals(item.getBusinessType()) ? 1 : 0);
        item.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
    }

    private void saveAttachment(Long userId, Long submissionId, Long requirementId, MultipartFile file) {
        String original = file.getOriginalFilename() == null ? "未命名文件" : file.getOriginalFilename();
        String extension = extension(original);
        String objectName = "doc-submissions/" + LocalDate.now() + "/" + UUID.randomUUID()
                + (extension.isBlank() ? "" : "." + extension);
        StoredObject stored = storageService.upload(file, objectName);
        SysDocAttachment attachment = new SysDocAttachment();
        attachment.setSubmissionId(submissionId);
        attachment.setRequirementId(requirementId);
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

    private void saveItemAttachment(Long userId, Long itemId, MultipartFile file) {
        String original = file.getOriginalFilename() == null ? "未命名文件" : file.getOriginalFilename();
        String extension = extension(original);
        String objectName = "doc-items/" + LocalDate.now() + "/" + UUID.randomUUID()
                + (extension.isBlank() ? "" : "." + extension);
        StoredObject stored = storageService.upload(file, objectName);
        SysDocItemAttachment attachment = new SysDocItemAttachment();
        attachment.setItemId(itemId);
        attachment.setOriginalFileName(original);
        attachment.setExtension(extension);
        attachment.setMimeType(stored.contentType());
        attachment.setFileSize(stored.size());
        attachment.setStorageBucket(stored.bucket());
        attachment.setStoragePath(stored.objectName());
        attachment.setUploadedBy(userId);
        attachment.setCreatedAt(LocalDateTime.now());
        itemAttachmentMapper.insert(attachment);
    }

    private List<SysDocAttachment> attachments(Long submissionId) {
        List<SysDocAttachment> attachments = attachmentMapper.selectList(new LambdaQueryWrapper<SysDocAttachment>()
                .eq(SysDocAttachment::getSubmissionId, submissionId)
                .orderByAsc(SysDocAttachment::getId));
        fillAttachmentInfo(attachments);
        return attachments;
    }

    private List<SysDocItemAttachment> itemAttachments(Long itemId) {
        return itemAttachmentMapper.selectList(new LambdaQueryWrapper<SysDocItemAttachment>()
                .eq(SysDocItemAttachment::getItemId, itemId)
                .orderByAsc(SysDocItemAttachment::getCreatedAt)
                .orderByAsc(SysDocItemAttachment::getId));
    }

    private void fillNodeItemInfo(List<SysDocNode> nodes) {
        Set<Long> itemIds = nodes.stream()
                .filter(node -> "FILE".equalsIgnoreCase(node.getNodeType()) && node.getItemId() != null)
                .map(SysDocNode::getItemId)
                .collect(Collectors.toSet());
        if (itemIds.isEmpty()) {
            return;
        }
        Map<Long, SysDocItem> itemMap = itemMapper.selectList(new LambdaQueryWrapper<SysDocItem>()
                        .in(SysDocItem::getId, itemIds)
                        .eq(SysDocItem::getDeleted, 0))
                .stream().collect(Collectors.toMap(SysDocItem::getId, Function.identity(), (a, b) -> a));
        for (SysDocNode node : nodes) {
            SysDocItem item = itemMap.get(node.getItemId());
            if (item == null) {
                continue;
            }
            node.setAttachmentEnabled(item.getAttachmentEnabled());
            node.setFileType(item.getFileType());
            node.setBusinessType(normalizeBusinessType(item));
            node.setSubmitterMode(normalizeSubmitterMode(item.getSubmitterMode()));
            node.setSubmissionCount(Math.toIntExact(submissionMapper.selectCount(new LambdaQueryWrapper<SysDocSubmission>()
                    .eq(SysDocSubmission::getItemId, item.getId()))));
        }
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
            item.setBusinessType(normalizeBusinessType(item));
            item.setSubmitterMode(normalizeSubmitterMode(item.getSubmitterMode()));
            SysDocCategory category = item.getCategoryId() == null ? null : categoryMap.get(item.getCategoryId());
            if (category != null) {
                item.setCategoryName(category.getCategoryName());
                item.setSectionDeptId(category.getSectionDeptId());
            }
            item.setSectionDeptName(name(deptMap.get(item.getSectionDeptId())));
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
            Long submitterDeptId = submission.getSubmitterDeptId() == null ? submission.getWorkshopDeptId() : submission.getSubmitterDeptId();
            submission.setSubmitterDeptName(submitterDeptId == null ? "无所属组织" : name(deptMap.get(submitterDeptId)));
            submission.setCategoryName(categoryMap.get(submission.getCategoryId()) == null ? null : categoryMap.get(submission.getCategoryId()).getCategoryName());
            submission.setItemName(itemMap.get(submission.getItemId()) == null ? null : itemMap.get(submission.getItemId()).getItemName());
            SysUser user = userMap.get(submission.getUploadUserId());
            submission.setUploadUserName(user == null ? null : (user.getRealName() == null ? user.getUsername() : user.getRealName()));
            submission.setAttachmentCount(Math.toIntExact(attachmentMapper.selectCount(new LambdaQueryWrapper<SysDocAttachment>()
                    .eq(SysDocAttachment::getSubmissionId, submission.getId()))));
        }
    }

    private List<SysDocUploadRequirement> requirements(Long itemId) {
        return requirementMapper.selectList(new LambdaQueryWrapper<SysDocUploadRequirement>()
                .eq(SysDocUploadRequirement::getItemId, itemId)
                .eq(SysDocUploadRequirement::getDeleted, 0)
                .orderByAsc(SysDocUploadRequirement::getSortOrder)
                .orderByAsc(SysDocUploadRequirement::getId));
    }

    private List<DocUploadRequirementRequest> requirementRequests(List<SysDocUploadRequirement> requirements) {
        if (requirements == null) {
            return List.of();
        }
        return requirements.stream()
                .map(item -> new DocUploadRequirementRequest(item.getId(), item.getRequirementName(), item.getDescription(), item.getSortOrder()))
                .toList();
    }

    private void replaceRequirements(Long itemId, List<DocUploadRequirementRequest> requests) {
        deleteRequirements(itemId);
        List<DocUploadRequirementRequest> source = requests == null ? List.of() : requests.stream()
                .filter(request -> request != null && request.requirementName() != null && !request.requirementName().trim().isBlank())
                .toList();
        if (source.isEmpty()) {
            source = List.of(new DocUploadRequirementRequest(null, "附件", null, 0));
        }
        int index = 0;
        for (DocUploadRequirementRequest request : source) {
            SysDocUploadRequirement requirement = new SysDocUploadRequirement();
            requirement.setItemId(itemId);
            requirement.setRequirementName(requiredText(request.requirementName(), "请输入收集项名称"));
            requirement.setDescription(optionalText(request.description()));
            requirement.setSortOrder(request.sortOrder() == null ? index : request.sortOrder());
            requirement.setCreatedAt(LocalDateTime.now());
            requirement.setUpdatedAt(LocalDateTime.now());
            requirement.setDeleted(0);
            requirementMapper.insert(requirement);
            index++;
        }
    }

    private void deleteRequirements(Long itemId) {
        requirementMapper.delete(new LambdaQueryWrapper<SysDocUploadRequirement>()
                .eq(SysDocUploadRequirement::getItemId, itemId));
    }

    private List<Long> normalizeSubmissionRequirementIds(List<SysDocUploadRequirement> requirements,
                                                         List<Long> requirementIds,
                                                         List<MultipartFile> uploadFiles) {
        if (requirements.isEmpty()) {
            throw new BusinessException("上传任务未配置收集项");
        }
        List<Long> requiredIds = requirements.stream().map(SysDocUploadRequirement::getId).toList();
        List<Long> submittedIds = requirementIds == null ? List.of() : requirementIds.stream()
                .filter(Objects::nonNull)
                .toList();
        if (submittedIds.isEmpty() && requiredIds.size() == 1 && uploadFiles.size() == 1) {
            return requiredIds;
        }
        if (submittedIds.size() != uploadFiles.size()) {
            throw new BusinessException("上传文件和收集项数量不一致");
        }
        Set<Long> requiredSet = Set.copyOf(requiredIds);
        Set<Long> submittedSet = Set.copyOf(submittedIds);
        if (submittedSet.size() != submittedIds.size() || !submittedSet.equals(requiredSet) || submittedIds.size() != requiredIds.size()) {
            throw new BusinessException("请按全部收集项上传附件");
        }
        return submittedIds;
    }

    private void requireSubmissionOpen(Long userId, SysDocItem item) {
        Long submittedCount = Objects.requireNonNullElse(submissionMapper.selectCount(new LambdaQueryWrapper<SysDocSubmission>()
                .eq(SysDocSubmission::getItemId, item.getId())), 0L);
        if ("SINGLE".equals(normalizeSubmitterMode(item.getSubmitterMode())) && submittedCount > 0) {
            throw new BusinessException("该上传任务已完成");
        }
        Long userSubmittedCount = Objects.requireNonNullElse(submissionMapper.selectCount(new LambdaQueryWrapper<SysDocSubmission>()
                .eq(SysDocSubmission::getItemId, item.getId())
                .eq(SysDocSubmission::getUploadUserId, userId)), 0L);
        if (userSubmittedCount > 0) {
            throw new BusinessException("您已提交过该上传任务");
        }
    }

    private Set<Long> completedUploadItemIds(Long userId, Long userDeptId, boolean superAdmin, Long sectionDeptId) {
        List<SysDocItem> uploadItems = itemMapper.selectList(new LambdaQueryWrapper<SysDocItem>()
                .eq(SysDocItem::getSectionDeptId, sectionDeptId)
                .eq(SysDocItem::getBusinessType, "UPLOAD")
                .eq(SysDocItem::getDeleted, 0));
        Set<Long> uploadItemIds = uploadItems.stream().map(SysDocItem::getId).collect(Collectors.toSet());
        if (uploadItemIds.isEmpty()) {
            return Set.of();
        }
        LambdaQueryWrapper<SysDocSubmission> wrapper = new LambdaQueryWrapper<SysDocSubmission>()
                .in(SysDocSubmission::getItemId, uploadItemIds);
        if (!canManageSection(userDeptId, superAdmin, sectionDeptId)) {
            if (userDeptId == null) {
                wrapper.eq(SysDocSubmission::getUploadUserId, userId);
            } else {
                wrapper.and(query -> query.eq(SysDocSubmission::getSubmitterDeptId, userDeptId)
                        .or()
                        .eq(SysDocSubmission::getUploadUserId, userId));
            }
        }
        return submissionMapper.selectList(wrapper).stream()
                .map(SysDocSubmission::getItemId)
                .collect(Collectors.toSet());
    }

    private int[] fillUploadProgress(SysDocNode node, Set<Long> completedItemIds) {
        if ("FILE".equalsIgnoreCase(node.getNodeType())) {
            int taskCount = "UPLOAD".equalsIgnoreCase(node.getBusinessType()) ? 1 : 0;
            int completedCount = taskCount == 1 && completedItemIds.contains(node.getItemId()) ? 1 : 0;
            node.setUploadTaskCount(taskCount);
            node.setCompletedUploadTaskCount(completedCount);
            node.setProgressPercent(taskCount == 0 ? 0 : completedCount * 100 / taskCount);
            return new int[]{taskCount, completedCount};
        }
        int taskCount = 0;
        int completedCount = 0;
        for (SysDocNode child : node.getChildren()) {
            int[] childProgress = fillUploadProgress(child, completedItemIds);
            taskCount += childProgress[0];
            completedCount += childProgress[1];
        }
        node.setUploadTaskCount(taskCount);
        node.setCompletedUploadTaskCount(completedCount);
        node.setProgressPercent(taskCount == 0 ? 0 : completedCount * 100 / taskCount);
        return new int[]{taskCount, completedCount};
    }

    private void fillAttachmentInfo(List<SysDocAttachment> attachments) {
        if (attachments.isEmpty()) {
            return;
        }
        Set<Long> requirementIds = attachments.stream()
                .map(SysDocAttachment::getRequirementId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SysDocUploadRequirement> requirementMap = requirementIds.isEmpty() ? Map.of() : requirementMapper.selectList(new LambdaQueryWrapper<SysDocUploadRequirement>()
                        .in(SysDocUploadRequirement::getId, requirementIds))
                .stream().collect(Collectors.toMap(SysDocUploadRequirement::getId, Function.identity(), (a, b) -> a));
        Set<Long> userIds = attachments.stream()
                .map(SysDocAttachment::getUploadedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Map.of() : userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .in(SysUser::getId, userIds)
                        .eq(SysUser::getDeleted, 0))
                .stream().collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));
        for (SysDocAttachment attachment : attachments) {
            SysDocUploadRequirement requirement = requirementMap.get(attachment.getRequirementId());
            attachment.setRequirementName(requirement == null ? null : requirement.getRequirementName());
            SysUser user = userMap.get(attachment.getUploadedBy());
            attachment.setUploadedByName(user == null ? null : displayName(user));
        }
    }

    private void requireSubmissionVisible(Long userId, Long userDeptId, boolean superAdmin, SysDocSubmission submission) {
        Long submitterDeptId = submission.getSubmitterDeptId() == null ? submission.getWorkshopDeptId() : submission.getSubmitterDeptId();
        if (superAdmin
                || Objects.equals(userId, submission.getUploadUserId())
                || Objects.equals(userDeptId, submission.getSectionDeptId())
                || Objects.equals(userDeptId, submitterDeptId)) {
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

    private NodePlacement resolvePlacement(Long userDeptId, boolean superAdmin, Long sectionDeptId, Long parentId) {
        if (parentId == null) {
            requireManageSection(userDeptId, superAdmin, sectionDeptId);
            return new NodePlacement(sectionDeptId, 1);
        }
        SysDocNode parent = requireNode(parentId);
        if (!"FOLDER".equalsIgnoreCase(parent.getNodeType())) {
            throw new BusinessException("只能在文件夹下新增内容");
        }
        requireManageSection(userDeptId, superAdmin, parent.getSectionDeptId());
        if (!Objects.equals(sectionDeptId, parent.getSectionDeptId())) {
            throw new BusinessException("父级目录不属于当前科室");
        }
        int level = parent.getLevel() == null ? 2 : parent.getLevel() + 1;
        if (level > 5) {
            throw new BusinessException("目录层级最多支持五层");
        }
        return new NodePlacement(parent.getSectionDeptId(), level);
    }

    private boolean hasUndeletedDescendant(SysDocNode folder) {
        List<SysDocNode> nodes = nodeMapper.selectList(new LambdaQueryWrapper<SysDocNode>()
                .eq(SysDocNode::getSectionDeptId, folder.getSectionDeptId()));
        Map<Long, SysDocNode> nodeMap = nodes.stream()
                .collect(Collectors.toMap(SysDocNode::getId, Function.identity(), (a, b) -> a));
        for (SysDocNode node : nodes) {
            if (Objects.equals(node.getId(), folder.getId()) || node.getDeleted() != null && node.getDeleted() == 1) {
                continue;
            }
            Long parentId = node.getParentId();
            while (parentId != null) {
                if (Objects.equals(parentId, folder.getId())) {
                    return true;
                }
                SysDocNode parent = nodeMap.get(parentId);
                if (parent == null) {
                    break;
                }
                parentId = parent.getParentId();
            }
        }
        return false;
    }

    private Long itemSectionDeptId(SysDocItem item) {
        if (item.getSectionDeptId() != null) {
            return item.getSectionDeptId();
        }
        return requireCategory(item.getCategoryId()).getSectionDeptId();
    }

    private Long itemSectionDeptId(SysDocItem item, SysDocCategory category) {
        if (item.getSectionDeptId() != null) {
            return item.getSectionDeptId();
        }
        if (category != null) {
            return category.getSectionDeptId();
        }
        throw new BusinessException("资料入口所属科室不存在");
    }

    private SysDept requireSection(Long sectionDeptId) {
        SysDept dept = deptMapper.selectById(sectionDeptId);
        if (dept == null || dept.getDeleted() != null && dept.getDeleted() == 1 || !isSection(dept)) {
            throw new BusinessException("科室不存在");
        }
        return dept;
    }

    private SysDept submitterDept(Long userDeptId) {
        if (userDeptId == null) {
            return null;
        }
        SysDept dept = deptMapper.selectById(userDeptId);
        if (dept == null || dept.getDeleted() != null && dept.getDeleted() == 1) {
            return null;
        }
        return dept;
    }

    private SysDocCategory requireCategory(Long id) {
        if (id == null) {
            throw new BusinessException("二级菜单不存在");
        }
        SysDocCategory category = categoryMapper.selectById(id);
        if (category == null || category.getDeleted() != null && category.getDeleted() == 1) {
            throw new BusinessException("二级菜单不存在");
        }
        return category;
    }

    private SysDocNode requireNode(Long id) {
        SysDocNode node = nodeMapper.selectById(id);
        if (node == null || node.getDeleted() != null && node.getDeleted() == 1) {
            throw new BusinessException("目录节点不存在");
        }
        return node;
    }

    private record NodePlacement(Long sectionDeptId, int level) {
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

    private String optionalText(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeFileType(String fileType) {
        if (fileType == null || fileType.trim().isBlank()) {
            return "OTHER";
        }
        String normalized = fileType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "WORD", "EXCEL", "PDF", "IMAGE", "OTHER" -> normalized;
            default -> "OTHER";
        };
    }

    private String normalizeBusinessType(SysDocItem item) {
        if (item.getBusinessType() == null || item.getBusinessType().trim().isBlank()) {
            return item.getAttachmentEnabled() != null && item.getAttachmentEnabled() == 1 ? "UPLOAD" : "ISSUED";
        }
        return normalizeBusinessType(item.getBusinessType());
    }

    private String normalizeBusinessType(String businessType) {
        if (businessType == null || businessType.trim().isBlank()) {
            return "ISSUED";
        }
        String normalized = businessType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "UPLOAD", "ISSUED" -> normalized;
            default -> "ISSUED";
        };
    }

    private String normalizeBusinessTypeOrNull(String businessType) {
        if (businessType == null || businessType.trim().isBlank()) {
            return null;
        }
        String normalized = businessType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "UPLOAD", "ISSUED" -> normalized;
            default -> null;
        };
    }

    private String normalizeSubmitterMode(String submitterMode) {
        if (submitterMode == null || submitterMode.trim().isBlank()) {
            return "SINGLE";
        }
        String normalized = submitterMode.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "SINGLE", "MULTIPLE" -> normalized;
            default -> "SINGLE";
        };
    }

    private boolean isUploadItem(SysDocItem item) {
        return "UPLOAD".equals(normalizeBusinessType(item));
    }

    private String extension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx >= 0 && idx < filename.length() - 1 ? filename.substring(idx + 1).toLowerCase(Locale.ROOT) : "";
    }

    private String name(SysDept dept) {
        return dept == null ? null : dept.getDeptName();
    }

    private String displayName(SysUser user) {
        if (user.getRealName() != null && !user.getRealName().isBlank()) {
            return user.getRealName();
        }
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            return user.getPhone();
        }
        return user.getUsername();
    }
}
