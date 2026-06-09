package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.dto.DocNodeRequest;
import cn.datong.standard.entity.SysDept;
import cn.datong.standard.entity.SysDocCategory;
import cn.datong.standard.entity.SysDocItem;
import cn.datong.standard.entity.SysDocNode;
import cn.datong.standard.entity.SysDocSubmission;
import cn.datong.standard.entity.SysDocUploadRequirement;
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
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocWorkspaceServiceTest {

    @Test
    void sectionNavigationReturnsOnlySections() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectList(any())).thenReturn(List.of(
                dept(1L, 0L, "机关", "AGENCY"),
                dept(2L, 1L, "办公室", "SECTION"),
                dept(3L, 1L, "技术科", "SECTION"),
                dept(5L, 0L, "房建车间", "WORKSHOP")
        ));

        assertThat(fx.service.sections()).extracting(item -> item.deptName())
                .containsExactly("办公室", "技术科");
    }

    @Test
    void documentTreeBuildsMixedFolderAndFileHierarchy() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        SysDocNode folder = node(1L, 2L, null, "FOLDER", "技术规范", null, 1, 10);
        SysDocNode file = node(2L, 2L, 1L, "FILE", "国铁集团技术规章", 8L, 2, 10);
        SysDocNode childFolder = node(3L, 2L, 1L, "FOLDER", "有效文件", null, 2, 20);
        when(fx.nodeMapper.selectList(any())).thenReturn(List.of(folder, file, childFolder));
        when(fx.submissionMapper.selectCount(any())).thenReturn(3L);
        when(fx.itemMapper.selectList(any())).thenReturn(List.of(item(8L, null, 2L, true, "EXCEL")));

        List<SysDocNode> tree = fx.service.documentTree(2L);

        assertThat(tree).extracting(SysDocNode::getNodeName).containsExactly("技术规范");
        assertThat(tree.getFirst().getChildren()).extracting(SysDocNode::getNodeName)
                .containsExactly("国铁集团技术规章", "有效文件");
        assertThat(tree.getFirst().getChildren().getFirst().getSubmissionCount()).isEqualTo(3);
        assertThat(tree.getFirst().getChildren().getFirst().getAttachmentEnabled()).isEqualTo(1);
        assertThat(tree.getFirst().getChildren().getFirst().getFileType()).isEqualTo("EXCEL");
    }

    @Test
    void createFileCreatesItemAndTreeNodeWithoutLegacyCategory() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        when(fx.itemMapper.insert(any(SysDocItem.class))).thenAnswer(invocation -> {
            SysDocItem item = invocation.getArgument(0);
            item.setId(88L);
            return 1;
        });
        DocNodeRequest request = new DocNodeRequest(2L, null, "新建资料", 30, null, true, "<p>内容</p>", "WORD");

        fx.service.createFileNode(10L, 2L, false, request);

        ArgumentCaptor<SysDocItem> itemCaptor = ArgumentCaptor.forClass(SysDocItem.class);
        verify(fx.itemMapper).insert(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getFileType()).isEqualTo("WORD");
        verify(fx.nodeMapper).insert(any(SysDocNode.class));
    }

    @Test
    void updateFileNodeCanChangeFileType() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        when(fx.nodeMapper.selectById(9L)).thenReturn(node(9L, 2L, null, "FILE", "旧文件", 88L, 1, 10));
        when(fx.itemMapper.selectById(88L)).thenReturn(item(88L, null, 2L, true, "WORD"));
        DocNodeRequest request = new DocNodeRequest(2L, null, "新文件", 20, null, true, "<p>新内容</p>", "PDF");

        fx.service.updateNode(2L, false, 9L, request);

        ArgumentCaptor<SysDocItem> itemCaptor = ArgumentCaptor.forClass(SysDocItem.class);
        verify(fx.itemMapper).updateById(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getFileType()).isEqualTo("PDF");
    }

    @Test
    void createFolderRejectsDepthGreaterThanFive() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        when(fx.nodeMapper.selectById(5L)).thenReturn(node(5L, 2L, 4L, "FOLDER", "第五层", null, 5, 10));
        DocNodeRequest request = new DocNodeRequest(2L, 5L, "第六层", 10, null, false, "", null);

        assertThatThrownBy(() -> fx.service.createFolderNode(10L, 2L, false, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("目录层级最多支持五层");
    }

    @Test
    void createFolderUnderParentOnlyInsertsChildNode() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        when(fx.nodeMapper.selectById(5L)).thenReturn(node(5L, 2L, null, "FOLDER", "父文件夹", null, 1, 10));
        DocNodeRequest request = new DocNodeRequest(2L, 5L, "子文件夹", 20, null, false, "", null);

        fx.service.createFolderNode(10L, 2L, false, request);

        ArgumentCaptor<SysDocNode> nodeCaptor = ArgumentCaptor.forClass(SysDocNode.class);
        verify(fx.nodeMapper).insert(nodeCaptor.capture());
        assertThat(nodeCaptor.getValue().getParentId()).isEqualTo(5L);
        assertThat(nodeCaptor.getValue().getLevel()).isEqualTo(2);
        assertThat(nodeCaptor.getValue().getSectionDeptId()).isEqualTo(2L);
        verify(fx.nodeMapper, never()).updateById(any(SysDocNode.class));
        verify(fx.nodeMapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteFolderRejectsWhenNestedUndeletedFileExists() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        when(fx.nodeMapper.selectById(5L)).thenReturn(node(5L, 2L, null, "FOLDER", "父文件夹", null, 1, 10));
        SysDocNode childFolder = node(6L, 2L, 5L, "FOLDER", "子文件夹", null, 2, 10);
        SysDocNode nestedFile = node(7L, 2L, 6L, "FILE", "未删除文件", 88L, 3, 10);
        when(fx.nodeMapper.selectList(any())).thenReturn(List.of(childFolder, nestedFile));

        assertThatThrownBy(() -> fx.service.deleteNode(2L, false, 5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件夹下存在未删除内容，无法删除");
    }

    @Test
    void sectionUserCanCreateOwnCategory() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        SysDocCategory request = new SysDocCategory();
        request.setSectionDeptId(2L);
        request.setCategoryName("房建大修");

        fx.service.createCategory(10L, 2L, false, request);

        verify(fx.categoryMapper).insert(any(SysDocCategory.class));
    }

    @Test
    void sectionUserCannotCreateOtherSectionCategory() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(3L)).thenReturn(dept(3L, 1L, "技术科", "SECTION"));
        SysDocCategory request = new SysDocCategory();
        request.setSectionDeptId(3L);
        request.setCategoryName("房建大修");

        assertThatThrownBy(() -> fx.service.createCategory(10L, 2L, false, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能维护本科室资料");
    }

    @Test
    void workshopUserCannotCreateCategory() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        SysDocCategory request = new SysDocCategory();
        request.setSectionDeptId(2L);
        request.setCategoryName("房建大修");

        assertThatThrownBy(() -> fx.service.createCategory(10L, 5L, false, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能维护本科室资料");
    }

    @Test
    void workshopSubmissionRequiresAttachmentEnabled() {
        Fixtures fx = fixtures();
        when(fx.itemMapper.selectById(8L)).thenReturn(item(8L, 6L, false));
        when(fx.categoryMapper.selectById(6L)).thenReturn(category(6L, 2L));
        when(fx.deptMapper.selectById(5L)).thenReturn(dept(5L, 0L, "房建车间", "WORKSHOP"));

        assertThatThrownBy(() -> fx.service.submit(20L, 5L, 8L, "{}", List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("该文件不是上传任务");
    }

    @Test
    void workshopSubmissionUploadsAttachmentWhenEnabled() {
        Fixtures fx = fixtures();
        when(fx.itemMapper.selectById(8L)).thenReturn(item(8L, 6L, true));
        when(fx.categoryMapper.selectById(6L)).thenReturn(category(6L, 2L));
        when(fx.deptMapper.selectById(5L)).thenReturn(dept(5L, 0L, "房建车间", "WORKSHOP"));
        when(fx.requirementMapper.selectList(any())).thenReturn(List.of(
                requirement(70L, 8L, "合同"),
                requirement(71L, 8L, "照片")
        ));
        when(fx.submissionMapper.selectById(any())).thenReturn(submission(1L, 8L, 6L, 2L, 5L, 5L));
        when(fx.submissionMapper.insert(any(SysDocSubmission.class))).thenAnswer(invocation -> {
            SysDocSubmission submission = invocation.getArgument(0);
            submission.setId(1L);
            return 1;
        });
        when(fx.deptMapper.selectList(any())).thenReturn(List.of());
        when(fx.userMapper.selectList(any())).thenReturn(List.of());
        when(fx.categoryMapper.selectList(any())).thenReturn(List.of());
        when(fx.itemMapper.selectList(any())).thenReturn(List.of());
        when(fx.attachmentMapper.selectCount(any())).thenReturn(1L);
        when(fx.attachmentMapper.selectList(any())).thenReturn(List.of());
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("合同.pdf");
        MultipartFile image = mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(false);
        when(image.getOriginalFilename()).thenReturn("照片.png");
        when(fx.storageService.upload(any(), any())).thenReturn(new StoredObject("standard-docs", "doc-submissions/test.pdf", 100L, "application/pdf"));

        fx.service.submit(20L, 5L, 8L, null, List.of(70L, 71L), List.of(file, image));

        verify(fx.attachmentMapper, times(2)).insert(any(cn.datong.standard.entity.SysDocAttachment.class));
    }

    @Test
    void sectionUserCanUploadOwnSectionAttachment() {
        Fixtures fx = fixtures();
        when(fx.itemMapper.selectById(8L)).thenReturn(item(8L, 6L, true));
        when(fx.categoryMapper.selectById(6L)).thenReturn(category(6L, 2L));
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        when(fx.requirementMapper.selectList(any())).thenReturn(List.of(requirement(70L, 8L, "附件")));
        when(fx.submissionMapper.selectById(any())).thenReturn(submission(1L, 8L, 6L, 2L, null, 2L));
        when(fx.submissionMapper.insert(any(SysDocSubmission.class))).thenAnswer(invocation -> {
            SysDocSubmission submission = invocation.getArgument(0);
            submission.setId(1L);
            return 1;
        });
        when(fx.deptMapper.selectList(any())).thenReturn(List.of(dept(2L, 1L, "办公室", "SECTION")));
        when(fx.userMapper.selectList(any())).thenReturn(List.of());
        when(fx.categoryMapper.selectList(any())).thenReturn(List.of(category(6L, 2L)));
        when(fx.itemMapper.selectList(any())).thenReturn(List.of(item(8L, 6L, true)));
        when(fx.attachmentMapper.selectCount(any())).thenReturn(1L);
        when(fx.attachmentMapper.selectList(any())).thenReturn(List.of());
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("说明.docx");
        when(fx.storageService.upload(any(), any())).thenReturn(new StoredObject("standard-docs", "doc-submissions/test.docx", 100L, "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));

        fx.service.submit(20L, 2L, 8L, null, List.of(file));

        verify(fx.submissionMapper).insert(any(SysDocSubmission.class));
        verify(fx.attachmentMapper).insert(any(cn.datong.standard.entity.SysDocAttachment.class));
    }

    @Test
    void singleSubmitterTaskLocksAfterFirstSubmission() {
        Fixtures fx = fixtures();
        when(fx.itemMapper.selectById(8L)).thenReturn(item(8L, 6L, true));
        when(fx.categoryMapper.selectById(6L)).thenReturn(category(6L, 2L));
        when(fx.deptMapper.selectById(5L)).thenReturn(dept(5L, 0L, "房建车间", "WORKSHOP"));
        when(fx.requirementMapper.selectList(any())).thenReturn(List.of(requirement(70L, 8L, "附件")));
        when(fx.submissionMapper.selectCount(any())).thenReturn(1L);
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("合同.pdf");

        assertThatThrownBy(() -> fx.service.submit(20L, 5L, 8L, null, List.of(file)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("该上传任务已完成");
    }

    @Test
    void sectionUserCanUploadOtherSectionAttachment() {
        Fixtures fx = fixtures();
        when(fx.itemMapper.selectById(8L)).thenReturn(item(8L, 6L, true));
        when(fx.categoryMapper.selectById(6L)).thenReturn(category(6L, 3L));
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        when(fx.requirementMapper.selectList(any())).thenReturn(List.of(requirement(70L, 8L, "附件")));
        when(fx.submissionMapper.selectById(any())).thenReturn(submission(1L, 8L, 6L, 3L, null, 2L));
        when(fx.submissionMapper.insert(any(SysDocSubmission.class))).thenAnswer(invocation -> {
            SysDocSubmission submission = invocation.getArgument(0);
            submission.setId(1L);
            return 1;
        });
        when(fx.deptMapper.selectList(any())).thenReturn(List.of(dept(2L, 1L, "办公室", "SECTION"), dept(3L, 1L, "技术科", "SECTION")));
        when(fx.userMapper.selectList(any())).thenReturn(List.of());
        when(fx.categoryMapper.selectList(any())).thenReturn(List.of(category(6L, 3L)));
        when(fx.itemMapper.selectList(any())).thenReturn(List.of(item(8L, 6L, true)));
        when(fx.attachmentMapper.selectCount(any())).thenReturn(1L);
        when(fx.attachmentMapper.selectList(any())).thenReturn(List.of());

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("跨科室.pdf");
        when(fx.storageService.upload(any(), any())).thenReturn(new StoredObject("standard-docs", "doc-submissions/test.pdf", 100L, "application/pdf"));

        fx.service.submit(20L, 2L, 8L, null, List.of(file));

        verify(fx.submissionMapper).insert(any(SysDocSubmission.class));
        verify(fx.attachmentMapper).insert(any(cn.datong.standard.entity.SysDocAttachment.class));
    }

    @Test
    void userWithoutDeptCanUploadAttachment() {
        Fixtures fx = fixtures();
        when(fx.itemMapper.selectById(8L)).thenReturn(item(8L, 6L, true));
        when(fx.categoryMapper.selectById(6L)).thenReturn(category(6L, 2L));
        when(fx.requirementMapper.selectList(any())).thenReturn(List.of(requirement(70L, 8L, "附件")));
        when(fx.submissionMapper.selectById(any())).thenReturn(submission(1L, 8L, 6L, 2L, null, null));
        when(fx.submissionMapper.insert(any(SysDocSubmission.class))).thenAnswer(invocation -> {
            SysDocSubmission submission = invocation.getArgument(0);
            submission.setId(1L);
            return 1;
        });
        when(fx.deptMapper.selectList(any())).thenReturn(List.of(dept(2L, 1L, "办公室", "SECTION")));
        when(fx.userMapper.selectList(any())).thenReturn(List.of());
        when(fx.categoryMapper.selectList(any())).thenReturn(List.of(category(6L, 2L)));
        when(fx.itemMapper.selectList(any())).thenReturn(List.of(item(8L, 6L, true)));
        when(fx.attachmentMapper.selectCount(any())).thenReturn(1L);
        when(fx.attachmentMapper.selectList(any())).thenReturn(List.of());

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("无组织.pdf");
        when(fx.storageService.upload(any(), any())).thenReturn(new StoredObject("standard-docs", "doc-submissions/test.pdf", 100L, "application/pdf"));

        fx.service.submit(1L, null, 8L, null, List.of(file));

        verify(fx.submissionMapper).insert(any(SysDocSubmission.class));
        verify(fx.attachmentMapper).insert(any(cn.datong.standard.entity.SysDocAttachment.class));
    }

    @Test
    void categorySubmissionQueryFiltersWorkshopRecords() {
        Fixtures fx = fixtures();
        when(fx.categoryMapper.selectById(6L)).thenReturn(category(6L, 2L));
        when(fx.deptMapper.selectById(5L)).thenReturn(dept(5L, 0L, "房建车间", "WORKSHOP"));
        SysDocSubmission own = new SysDocSubmission();
        own.setId(1L);
        own.setCategoryId(6L);
        own.setWorkshopDeptId(5L);
        own.setSubmitterDeptId(5L);
        when(fx.submissionMapper.selectList(any())).thenReturn(List.of(own));
        when(fx.deptMapper.selectList(any())).thenReturn(List.of());
        when(fx.userMapper.selectList(any())).thenReturn(List.of());
        when(fx.categoryMapper.selectList(any())).thenReturn(List.of());
        when(fx.itemMapper.selectList(any())).thenReturn(List.of());
        when(fx.attachmentMapper.selectCount(any())).thenReturn(0L);

        assertThat(fx.service.submissions(20L, 5L, false, 6L)).containsExactly(own);
    }

    @Test
    void itemSubmissionQueryRejectsWorkshopRecordList() {
        Fixtures fx = fixtures();
        when(fx.itemMapper.selectById(8L)).thenReturn(item(8L, 6L, true));
        when(fx.categoryMapper.selectById(6L)).thenReturn(category(6L, 2L));
        when(fx.deptMapper.selectById(5L)).thenReturn(dept(5L, 0L, "房建车间", "WORKSHOP"));

        assertThatThrownBy(() -> fx.service.itemSubmissions(20L, 5L, false, 8L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只有科室用户可以查看全部上传记录");
    }

    @Test
    void sectionUserCanUploadIssuedItemAttachment() {
        Fixtures fx = fixtures();
        when(fx.itemMapper.selectById(9L)).thenReturn(item(9L, null, 2L, false));
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        when(fx.itemAttachmentMapper.selectList(any())).thenReturn(List.of());
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("通知.pdf");
        when(fx.storageService.upload(any(), any())).thenReturn(new StoredObject("standard-docs", "doc-items/test.pdf", 100L, "application/pdf"));

        fx.service.addItemAttachments(20L, 2L, false, 9L, List.of(file));

        verify(fx.itemAttachmentMapper).insert(any(cn.datong.standard.entity.SysDocItemAttachment.class));
    }

    private Fixtures fixtures() {
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysDocCategoryMapper categoryMapper = mock(SysDocCategoryMapper.class);
        SysDocItemMapper itemMapper = mock(SysDocItemMapper.class);
        SysDocNodeMapper nodeMapper = mock(SysDocNodeMapper.class);
        SysDocSubmissionMapper submissionMapper = mock(SysDocSubmissionMapper.class);
        SysDocAttachmentMapper attachmentMapper = mock(SysDocAttachmentMapper.class);
        SysDocUploadRequirementMapper requirementMapper = mock(SysDocUploadRequirementMapper.class);
        SysDocItemAttachmentMapper itemAttachmentMapper = mock(SysDocItemAttachmentMapper.class);
        FileStorageService storageService = mock(FileStorageService.class);
        DocWorkspaceService service = new DocWorkspaceService(
                deptMapper,
                userMapper,
                categoryMapper,
                itemMapper,
                nodeMapper,
                submissionMapper,
                attachmentMapper,
                requirementMapper,
                itemAttachmentMapper,
                storageService
        );
        return new Fixtures(deptMapper, userMapper, categoryMapper, itemMapper, nodeMapper, submissionMapper, attachmentMapper, requirementMapper, itemAttachmentMapper, storageService, service);
    }

    private SysDept dept(Long id, Long parentId, String name, String type) {
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setParentId(parentId);
        dept.setDeptName(name);
        dept.setDeptCode("D" + id);
        dept.setDeptType(type);
        dept.setStatus("ENABLED");
        dept.setDeleted(0);
        return dept;
    }

    private SysDocCategory category(Long id, Long sectionDeptId) {
        SysDocCategory category = new SysDocCategory();
        category.setId(id);
        category.setSectionDeptId(sectionDeptId);
        category.setCategoryName("房建大修");
        category.setDeleted(0);
        return category;
    }

    private SysDocItem item(Long id, Long categoryId, boolean attachment) {
        SysDocItem item = new SysDocItem();
        item.setId(id);
        item.setCategoryId(categoryId);
        item.setItemName("施工合同");
        item.setAttachmentEnabled(attachment ? 1 : 0);
        item.setBusinessType(attachment ? "UPLOAD" : "ISSUED");
        item.setSubmitterMode("SINGLE");
        item.setDeleted(0);
        return item;
    }

    private SysDocItem item(Long id, Long categoryId, Long sectionDeptId, boolean attachment) {
        SysDocItem item = item(id, categoryId, attachment);
        item.setSectionDeptId(sectionDeptId);
        return item;
    }

    private SysDocItem item(Long id, Long categoryId, Long sectionDeptId, boolean attachment, String fileType) {
        SysDocItem item = item(id, categoryId, sectionDeptId, attachment);
        item.setFileType(fileType);
        return item;
    }

    private SysDocNode node(Long id, Long sectionDeptId, Long parentId, String nodeType, String nodeName,
                            Long itemId, Integer level, Integer sortOrder) {
        SysDocNode node = new SysDocNode();
        node.setId(id);
        node.setSectionDeptId(sectionDeptId);
        node.setParentId(parentId);
        node.setNodeType(nodeType);
        node.setNodeName(nodeName);
        node.setItemId(itemId);
        node.setLevel(level);
        node.setSortOrder(sortOrder);
        node.setDeleted(0);
        return node;
    }

    private SysDocUploadRequirement requirement(Long id, Long itemId, String name) {
        SysDocUploadRequirement requirement = new SysDocUploadRequirement();
        requirement.setId(id);
        requirement.setItemId(itemId);
        requirement.setRequirementName(name);
        requirement.setSortOrder(0);
        requirement.setDeleted(0);
        return requirement;
    }

    private SysDocSubmission submission(Long id, Long itemId, Long categoryId, Long sectionDeptId, Long workshopDeptId, Long submitterDeptId) {
        SysDocSubmission submission = new SysDocSubmission();
        submission.setId(id);
        submission.setItemId(itemId);
        submission.setCategoryId(categoryId);
        submission.setSectionDeptId(sectionDeptId);
        submission.setWorkshopDeptId(workshopDeptId);
        submission.setSubmitterDeptId(submitterDeptId);
        submission.setUploadUserId(20L);
        return submission;
    }

    private record Fixtures(
            SysDeptMapper deptMapper,
            SysUserMapper userMapper,
            SysDocCategoryMapper categoryMapper,
            SysDocItemMapper itemMapper,
            SysDocNodeMapper nodeMapper,
            SysDocSubmissionMapper submissionMapper,
            SysDocAttachmentMapper attachmentMapper,
            SysDocUploadRequirementMapper requirementMapper,
            SysDocItemAttachmentMapper itemAttachmentMapper,
            FileStorageService storageService,
            DocWorkspaceService service
    ) {
    }
}
