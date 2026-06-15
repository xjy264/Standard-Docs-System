package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.dto.DocNodeRequest;
import cn.datong.standard.dto.DocUploadRequirementRequest;
import cn.datong.standard.entity.SysDept;
import cn.datong.standard.entity.SysDocCategory;
import cn.datong.standard.entity.SysDocItem;
import cn.datong.standard.entity.SysDocItemAttachment;
import cn.datong.standard.entity.SysDocNode;
import cn.datong.standard.entity.SysDocSubmission;
import cn.datong.standard.entity.SysDocUploadRequirement;
import cn.datong.standard.entity.SysDocItemWorkshopScope;
import cn.datong.standard.entity.SysRepairProjectTemplate;
import cn.datong.standard.entity.SysRepairProjectTemplateItem;
import cn.datong.standard.mapper.SysDeptMapper;
import cn.datong.standard.mapper.SysDocAttachmentMapper;
import cn.datong.standard.mapper.SysDocCategoryMapper;
import cn.datong.standard.mapper.SysDocItemMapper;
import cn.datong.standard.mapper.SysDocItemAttachmentMapper;
import cn.datong.standard.mapper.SysDocNodeMapper;
import cn.datong.standard.mapper.SysDocSubmissionMapper;
import cn.datong.standard.mapper.SysDocItemWorkshopScopeMapper;
import cn.datong.standard.mapper.SysDocUploadRequirementMapper;
import cn.datong.standard.mapper.SysRepairProjectTemplateItemMapper;
import cn.datong.standard.mapper.SysRepairProjectTemplateMapper;
import cn.datong.standard.mapper.SysUserMapper;
import cn.datong.standard.storage.FileStorageService;
import cn.datong.standard.storage.StoredObject;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
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
        when(fx.itemMapper.selectList(any())).thenReturn(List.of(item(8L, null, 2L, true, "EXCEL", 2026)));

        List<SysDocNode> tree = fx.service.documentTree(2L);

        assertThat(tree).extracting(SysDocNode::getNodeName).containsExactly("技术规范");
        assertThat(tree.getFirst().getChildren()).extracting(SysDocNode::getNodeName)
                .containsExactly("国铁集团技术规章", "有效文件");
        assertThat(tree.getFirst().getChildren().getFirst().getSubmissionCount()).isEqualTo(3);
        assertThat(tree.getFirst().getChildren().getFirst().getAttachmentEnabled()).isEqualTo(1);
        assertThat(tree.getFirst().getChildren().getFirst().getFileType()).isEqualTo("EXCEL");
        assertThat(tree.getFirst().getChildren().getFirst().getDocYear()).isEqualTo(2026);
    }

    @Test
    void uploadProgressCountsOnlyUploadItemsWithRequirements() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        SysDocNode folder = node(1L, 2L, null, "FOLDER", "资料收集", null, 1, 10);
        SysDocNode fileWithRequirement = node(2L, 2L, 1L, "FILE", "车间成员信息表", 8L, 2, 10);
        SysDocNode fileWithoutRequirement = node(3L, 2L, 1L, "FILE", "普通说明", 9L, 2, 20);
        when(fx.nodeMapper.selectList(any())).thenReturn(List.of(folder, fileWithRequirement, fileWithoutRequirement));
        when(fx.itemMapper.selectList(any())).thenReturn(List.of(
                item(8L, null, 2L, true),
                item(9L, null, 2L, true)
        ));
        when(fx.requirementMapper.selectList(any())).thenReturn(List.of(requirement(70L, 8L, "附件")));
        when(fx.submissionMapper.selectCount(any())).thenReturn(0L);
        when(fx.submissionMapper.selectList(any())).thenReturn(List.of(submission(1L, 8L, null, 2L, 5L, 5L)));

        List<SysDocNode> tree = fx.service.documentTree(5L, 5L, false, 2L, "UPLOAD");

        SysDocNode root = tree.getFirst();
        assertThat(root.getUploadTaskCount()).isEqualTo(1);
        assertThat(root.getCompletedUploadTaskCount()).isEqualTo(1);
        assertThat(root.getProgressPercent()).isEqualTo(100);
    }

    @Test
    void unifiedDocumentTreeCalculatesUploadProgress() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        SysDocNode folder = node(1L, 2L, null, "FOLDER", "资料目录", null, 1, 10);
        SysDocNode uploadFile = node(2L, 2L, 1L, "FILE", "车间成员信息表", 8L, 2, 10);
        SysDocNode issuedFile = node(3L, 2L, 1L, "FILE", "公示通知", 9L, 2, 20);
        when(fx.nodeMapper.selectList(any())).thenReturn(List.of(folder, uploadFile, issuedFile));
        when(fx.itemMapper.selectList(any())).thenReturn(List.of(
                item(8L, null, 2L, true),
                item(9L, null, 2L, false)
        ));
        when(fx.requirementMapper.selectList(any())).thenReturn(List.of(requirement(70L, 8L, "附件")));
        when(fx.submissionMapper.selectCount(any())).thenReturn(0L);
        when(fx.submissionMapper.selectList(any())).thenReturn(List.of(submission(1L, 8L, null, 2L, 5L, 5L)));

        List<SysDocNode> tree = fx.service.documentTree(5L, 5L, false, 2L, null);

        SysDocNode root = tree.getFirst();
        assertThat(root.getChildren()).extracting(SysDocNode::getNodeName)
                .containsExactly("车间成员信息表", "公示通知");
        assertThat(root.getUploadTaskCount()).isEqualTo(1);
        assertThat(root.getCompletedUploadTaskCount()).isEqualTo(1);
        assertThat(root.getProgressPercent()).isEqualTo(100);
    }

    @Test
    void createFileRejectsRootLevelFile() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        DocNodeRequest request = new DocNodeRequest(2L, null, "新建资料", 30, null, true, "<p>内容</p>", "WORD", 2026);

        assertThatThrownBy(() -> fx.service.createFileNode(10L, 2L, false, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能在文件夹下新建文件");
    }

    @Test
    void createFileWithMainFileInfersExcelTypeAndSavesOneMainAttachment() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        when(fx.nodeMapper.selectById(5L)).thenReturn(node(5L, 2L, null, "FOLDER", "资料夹", null, 1, 10));
        when(fx.storageService.upload(any(), any())).thenReturn(new StoredObject("standard-docs", "doc-items/test.xlsx", 100L, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        when(fx.itemMapper.insert(any(SysDocItem.class))).thenAnswer(invocation -> {
            SysDocItem item = invocation.getArgument(0);
            item.setId(88L);
            return 1;
        });
        MultipartFile file = new MockMultipartFile("file", "计划表.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "demo".getBytes());
        DocNodeRequest request = new DocNodeRequest(2L, 5L, "计划表", 30, null, false, "", null, 2026);

        fx.service.createFileNodeWithMainFile(10L, 2L, false, request, file);

        ArgumentCaptor<SysDocItem> itemCaptor = ArgumentCaptor.forClass(SysDocItem.class);
        verify(fx.itemMapper).insert(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getFileType()).isEqualTo("EXCEL");
        assertThat(itemCaptor.getValue().getDocYear()).isEqualTo(2026);
        ArgumentCaptor<SysDocItemAttachment> attachmentCaptor = ArgumentCaptor.forClass(SysDocItemAttachment.class);
        verify(fx.itemAttachmentMapper).insert(attachmentCaptor.capture());
        assertThat(attachmentCaptor.getValue().getItemId()).isEqualTo(88L);
        assertThat(attachmentCaptor.getValue().getOriginalFileName()).isEqualTo("计划表.xlsx");
        verify(fx.nodeMapper).insert(any(SysDocNode.class));
    }

    @Test
    void createFileWithMainFileInheritsParentFolderYearWhenDocYearMissing() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        SysDocNode parent = node(5L, 2L, null, "FOLDER", "2028年度资料", null, 1, 10);
        parent.setDocYear(2028);
        when(fx.nodeMapper.selectById(5L)).thenReturn(parent);
        when(fx.storageService.upload(any(), any())).thenReturn(new StoredObject("standard-docs", "doc-items/test.pdf", 100L, "application/pdf"));
        when(fx.itemMapper.insert(any(SysDocItem.class))).thenAnswer(invocation -> {
            SysDocItem item = invocation.getArgument(0);
            item.setId(88L);
            return 1;
        });
        MultipartFile file = new MockMultipartFile("file", "通知.pdf", "application/pdf", "demo".getBytes());
        DocNodeRequest request = new DocNodeRequest(2L, 5L, "通知", 30, null, false, "", null, null);

        fx.service.createFileNodeWithMainFile(10L, 2L, false, request, file);

        ArgumentCaptor<SysDocItem> itemCaptor = ArgumentCaptor.forClass(SysDocItem.class);
        verify(fx.itemMapper).insert(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getDocYear()).isEqualTo(2028);
    }

    @Test
    void createFileWithMainFileDoesNotInsertRowsWhenStorageFails() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        when(fx.nodeMapper.selectById(5L)).thenReturn(node(5L, 2L, null, "FOLDER", "资料夹", null, 1, 10));
        when(fx.storageService.upload(any(), any())).thenThrow(new BusinessException("文件上传失败：无法连接 MinIO 存储服务，请检查 9000 端口"));
        MultipartFile file = new MockMultipartFile("file", "通知.pdf", "application/pdf", "demo".getBytes());
        DocNodeRequest request = new DocNodeRequest(2L, 5L, "通知", 30, null, false, "", null, 2026);

        assertThatThrownBy(() -> fx.service.createFileNodeWithMainFile(10L, 2L, false, request, file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无法连接 MinIO");
        verify(fx.itemMapper, never()).insert(any(SysDocItem.class));
        verify(fx.nodeMapper, never()).insert(any(SysDocNode.class));
        verify(fx.itemAttachmentMapper, never()).insert(any(SysDocItemAttachment.class));
    }

    @Test
    void createFileRejectsMissingDocYearWhenParentFolderHasNoYear() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        SysDocNode parent = node(5L, 2L, null, "FOLDER", "资料夹", null, 1, 10);
        parent.setDocYear(null);
        when(fx.nodeMapper.selectById(5L)).thenReturn(parent);
        DocNodeRequest request = new DocNodeRequest(2L, 5L, "新建资料", 30, null, true, "<p>内容</p>", "WORD", null);

        assertThatThrownBy(() -> fx.service.createFileNode(10L, 2L, false, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请选择文件年份");
    }

    @Test
    void createFileRejectsInvalidDocYear() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        when(fx.nodeMapper.selectById(5L)).thenReturn(node(5L, 2L, null, "FOLDER", "资料夹", null, 1, 10));
        DocNodeRequest request = new DocNodeRequest(2L, 5L, "新建资料", 30, null, true, "<p>内容</p>", "WORD", 99);

        assertThatThrownBy(() -> fx.service.createFileNode(10L, 2L, false, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件年份必须为四位年份");
    }

    @Test
    void createFolderPersistsProgressVisibility() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        when(fx.orgAssignmentService.adminUserIds()).thenReturn(java.util.Set.of(10L));
        DocNodeRequest request = new DocNodeRequest(2L, null, "资料收集", 10, null, false, "", null, 2026, null, null, false);

        fx.service.createFolderNode(10L, 2L, false, request);

        ArgumentCaptor<SysDocNode> nodeCaptor = ArgumentCaptor.forClass(SysDocNode.class);
        verify(fx.nodeMapper).insert(nodeCaptor.capture());
        assertThat(nodeCaptor.getValue().getShowUploadProgress()).isEqualTo(0);
    }

    @Test
    void updateFolderPersistsProgressVisibility() {
        Fixtures fx = fixtures();
        when(fx.nodeMapper.selectById(5L)).thenReturn(node(5L, 2L, null, "FOLDER", "资料收集", null, 1, 10));
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        DocNodeRequest request = new DocNodeRequest(2L, null, "资料收集", 10, null, false, "", null, null, null, null, false);

        fx.service.updateNode(2L, false, 5L, request);

        ArgumentCaptor<SysDocNode> nodeCaptor = ArgumentCaptor.forClass(SysDocNode.class);
        verify(fx.nodeMapper).updateById(nodeCaptor.capture());
        assertThat(nodeCaptor.getValue().getShowUploadProgress()).isEqualTo(0);
    }

    @Test
    void createUploadFilePersistsRequirementDescription() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        when(fx.itemMapper.insert(any(SysDocItem.class))).thenAnswer(invocation -> {
            SysDocItem item = invocation.getArgument(0);
            item.setId(88L);
            return 1;
        });
        DocNodeRequest request = new DocNodeRequest(
                2L,
                5L,
                "车间成员信息收集表",
                30,
                null,
                true,
                "",
                "OTHER",
                2026,
                "UPLOAD",
                "MULTIPLE",
                List.of(new DocUploadRequirementRequest(null, "成员信息表", "请上传盖章后的 Excel 文件", 0))
        );
        when(fx.nodeMapper.selectById(5L)).thenReturn(node(5L, 2L, null, "FOLDER", "资料夹", null, 1, 10));

        fx.service.createFileNode(10L, 2L, false, request);

        ArgumentCaptor<SysDocUploadRequirement> requirementCaptor = ArgumentCaptor.forClass(SysDocUploadRequirement.class);
        verify(fx.requirementMapper).insert(requirementCaptor.capture());
        assertThat(requirementCaptor.getValue().getRequirementName()).isEqualTo("成员信息表");
        assertThat(requirementCaptor.getValue().getDescription()).isEqualTo("请上传盖章后的 Excel 文件");
    }

    @Test
    void createFilePersistsUnifiedUploadDeadlineAndVisibleWorkshops() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        when(fx.deptMapper.selectById(5L)).thenReturn(dept(5L, 0L, "房建车间", "WORKSHOP"));
        when(fx.itemMapper.insert(any(SysDocItem.class))).thenAnswer(invocation -> {
            SysDocItem item = invocation.getArgument(0);
            item.setId(88L);
            return 1;
        });
        LocalDateTime deadline = LocalDateTime.now().plusDays(3);
        DocNodeRequest request = new DocNodeRequest(
                2L,
                5L,
                "施工通知",
                30,
                null,
                true,
                "",
                "PDF",
                2026,
                null,
                null,
                List.of(),
                deadline,
                true,
                List.of(5L)
        );
        when(fx.nodeMapper.selectById(5L)).thenReturn(node(5L, 2L, null, "FOLDER", "资料夹", null, 1, 10));

        fx.service.createFileNode(10L, 2L, false, request);

        ArgumentCaptor<SysDocItem> itemCaptor = ArgumentCaptor.forClass(SysDocItem.class);
        verify(fx.itemMapper).insert(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getBusinessType()).isEqualTo("UPLOAD");
        assertThat(itemCaptor.getValue().getWorkshopUploadEnabled()).isEqualTo(1);
        assertThat(itemCaptor.getValue().getUploadDeadline()).isEqualTo(deadline);
        assertThat(itemCaptor.getValue().getVisibilityScope()).isEqualTo("SELECTED");
        ArgumentCaptor<SysDocItemWorkshopScope> scopeCaptor = ArgumentCaptor.forClass(SysDocItemWorkshopScope.class);
        verify(fx.itemWorkshopScopeMapper).insert(scopeCaptor.capture());
        assertThat(scopeCaptor.getValue().getItemId()).isEqualTo(88L);
        assertThat(scopeCaptor.getValue().getWorkshopDeptId()).isEqualTo(5L);
    }

    @Test
    void workshopDocumentTreeOnlyShowsVisibleFiles() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        when(fx.deptMapper.selectById(5L)).thenReturn(dept(5L, 0L, "房建车间", "WORKSHOP"));
        SysDocNode folder = node(1L, 2L, null, "FOLDER", "通知", null, 1, 10);
        SysDocNode visible = node(2L, 2L, 1L, "FILE", "可见文件", 8L, 2, 10);
        SysDocNode hidden = node(3L, 2L, 1L, "FILE", "不可见文件", 9L, 2, 20);
        SysDocItem visibleItem = item(8L, null, 2L, true, "PDF", 2026);
        visibleItem.setVisibilityScope("SELECTED");
        SysDocItem hiddenItem = item(9L, null, 2L, true, "PDF", 2026);
        hiddenItem.setVisibilityScope("SELECTED");
        when(fx.nodeMapper.selectList(any())).thenReturn(List.of(folder, visible, hidden));
        when(fx.itemMapper.selectList(any())).thenReturn(List.of(visibleItem, hiddenItem));
        when(fx.itemWorkshopScopeMapper.selectList(any())).thenReturn(List.of(scope(8L, 5L), scope(9L, 6L)));
        when(fx.submissionMapper.selectCount(any())).thenReturn(0L);

        List<SysDocNode> tree = fx.service.documentTree(20L, 5L, false, 2L, null);

        assertThat(tree).extracting(SysDocNode::getNodeName).containsExactly("通知");
        assertThat(tree.getFirst().getChildren()).extracting(SysDocNode::getNodeName).containsExactly("可见文件");
    }

    @Test
    void workshopSubmissionRejectsAfterDeadline() {
        Fixtures fx = fixtures();
        SysDocItem item = item(8L, 6L, true);
        item.setWorkshopUploadEnabled(1);
        item.setUploadDeadline(LocalDateTime.now().minusMinutes(1));
        when(fx.itemMapper.selectById(8L)).thenReturn(item);
        when(fx.categoryMapper.selectById(6L)).thenReturn(category(6L, 2L));
        when(fx.deptMapper.selectById(5L)).thenReturn(dept(5L, 0L, "房建车间", "WORKSHOP"));
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("合同.pdf");

        assertThatThrownBy(() -> fx.service.submit(20L, 5L, 8L, null, List.of(file)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已超过上传截止时间");
    }

    @Test
    void repairTemplateImportRejectsRepairRootItself() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(3L)).thenReturn(dept(3L, 1L, "技术科", "SECTION"));
        when(fx.nodeMapper.selectById(10L)).thenReturn(node(10L, 3L, null, "FOLDER", "房建大修", null, 1, 10));
        when(fx.orgAssignmentService.adminUserIds()).thenReturn(java.util.Set.of(20L));

        assertThatThrownBy(() -> fx.service.importRepairTemplateFiles(20L, 3L, false, 10L, List.of(1000L), 2026))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能在房建大修子文件夹下导入模板文件");
    }

    @Test
    void repairTemplateImportCopiesSelectedTemplateFilesIntoExistingFolder() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(3L)).thenReturn(dept(3L, 1L, "技术科", "SECTION"));
        when(fx.nodeMapper.selectById(10L)).thenReturn(node(10L, 3L, null, "FOLDER", "房建大修", null, 1, 10));
        when(fx.nodeMapper.selectById(11L)).thenReturn(node(11L, 3L, 10L, "FOLDER", "2026年站房大修", null, 2, 10));
        when(fx.orgAssignmentService.adminUserIds()).thenReturn(java.util.Set.of(20L));
        SysRepairProjectTemplate template = new SysRepairProjectTemplate();
        template.setId(100L);
        template.setTemplateName("大修项目标准模板");
        template.setDeleted(0);
        SysRepairProjectTemplateItem contract = templateItem(100L, "施工合同", 10);
        contract.setId(1000L);
        contract.setFileType("WORD");
        contract.setOriginalFileName("施工合同.docx");
        contract.setExtension("docx");
        contract.setMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        contract.setFileSize(100L);
        contract.setStorageBucket("standard-docs");
        contract.setStoragePath("repair-template/contract.docx");
        SysRepairProjectTemplateItem report = templateItem(100L, "开工报告", 20);
        report.setId(1001L);
        report.setFileType("PDF");
        report.setOriginalFileName("开工报告.pdf");
        report.setExtension("pdf");
        report.setMimeType("application/pdf");
        report.setFileSize(80L);
        report.setStorageBucket("standard-docs");
        report.setStoragePath("repair-template/report.pdf");
        when(fx.repairTemplateItemMapper.selectList(any())).thenReturn(List.of(contract, report));
        when(fx.storageService.download(any(), any())).thenReturn(new java.io.ByteArrayInputStream("demo".getBytes()));
        when(fx.storageService.upload(any(), any())).thenAnswer(invocation -> new StoredObject("standard-docs", "doc-items/copied-" + java.util.UUID.randomUUID(), 100L, "application/octet-stream"));
        when(fx.itemMapper.insert(any(SysDocItem.class))).thenAnswer(invocation -> {
            SysDocItem item = invocation.getArgument(0);
            item.setId(item.getSortOrder().longValue());
            return 1;
        });

        fx.service.importRepairTemplateFiles(20L, 3L, false, 11L, List.of(1000L, 1001L), 2026);

        ArgumentCaptor<SysDocNode> nodeCaptor = ArgumentCaptor.forClass(SysDocNode.class);
        verify(fx.nodeMapper, times(2)).insert(nodeCaptor.capture());
        assertThat(nodeCaptor.getAllValues()).extracting(SysDocNode::getNodeName)
                .containsExactly("施工合同", "开工报告");
        verify(fx.itemMapper, times(2)).insert(any(SysDocItem.class));
        verify(fx.itemAttachmentMapper, times(2)).insert(any(SysDocItemAttachment.class));
    }

    @Test
    void updateFileNodeCanChangeFileType() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        when(fx.nodeMapper.selectById(9L)).thenReturn(node(9L, 2L, 5L, "FILE", "旧文件", 88L, 2, 10));
        when(fx.itemMapper.selectById(88L)).thenReturn(item(88L, null, 2L, true, "WORD", 2025));
        DocNodeRequest request = new DocNodeRequest(2L, null, "新文件", 20, null, true, "<p>新内容</p>", "PDF", 2026);

        fx.service.updateNode(2L, false, 9L, request);

        ArgumentCaptor<SysDocItem> itemCaptor = ArgumentCaptor.forClass(SysDocItem.class);
        verify(fx.itemMapper).updateById(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getFileType()).isEqualTo("PDF");
        assertThat(itemCaptor.getValue().getDocYear()).isEqualTo(2026);
    }

    @Test
    void createFolderRejectsDepthGreaterThanFive() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        when(fx.nodeMapper.selectById(5L)).thenReturn(node(5L, 2L, 4L, "FOLDER", "第五层", null, 5, 10));
        DocNodeRequest request = new DocNodeRequest(2L, 5L, "第六层", 10, null, false, "", null, null);

        assertThatThrownBy(() -> fx.service.createFolderNode(10L, 2L, false, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("目录层级最多支持五层");
    }

    @Test
    void createFolderUnderParentOnlyInsertsChildNode() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        when(fx.nodeMapper.selectById(5L)).thenReturn(node(5L, 2L, null, "FOLDER", "父文件夹", null, 1, 10));
        DocNodeRequest request = new DocNodeRequest(2L, 5L, "子文件夹", 20, null, false, "", null, null);

        fx.service.createFolderNode(10L, 2L, false, request);

        ArgumentCaptor<SysDocNode> nodeCaptor = ArgumentCaptor.forClass(SysDocNode.class);
        verify(fx.nodeMapper).insert(nodeCaptor.capture());
        assertThat(nodeCaptor.getValue().getParentId()).isEqualTo(5L);
        assertThat(nodeCaptor.getValue().getDocYear()).isEqualTo(2026);
        assertThat(nodeCaptor.getValue().getLevel()).isEqualTo(2);
        assertThat(nodeCaptor.getValue().getSectionDeptId()).isEqualTo(2L);
        verify(fx.nodeMapper, never()).updateById(any(SysDocNode.class));
        verify(fx.nodeMapper, never()).deleteById(anyLong());
    }

    @Test
    void sectionAdminCanCreateOwnRootFolder() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        when(fx.orgAssignmentService.adminUserIds()).thenReturn(java.util.Set.of(10L));
        DocNodeRequest request = new DocNodeRequest(2L, null, "年度资料", 20, null, false, "", null, 2028);

        fx.service.createFolderNode(10L, 2L, false, request);

        ArgumentCaptor<SysDocNode> nodeCaptor = ArgumentCaptor.forClass(SysDocNode.class);
        verify(fx.nodeMapper).insert(nodeCaptor.capture());
        assertThat(nodeCaptor.getValue().getParentId()).isNull();
        assertThat(nodeCaptor.getValue().getDocYear()).isEqualTo(2028);
        assertThat(nodeCaptor.getValue().getLevel()).isEqualTo(1);
        assertThat(nodeCaptor.getValue().getSectionDeptId()).isEqualTo(2L);
    }

    @Test
    void normalSectionUserCannotCreateRootFolder() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
        when(fx.orgAssignmentService.adminUserIds()).thenReturn(java.util.Set.of());
        DocNodeRequest request = new DocNodeRequest(2L, null, "年度资料", 20, null, false, "", null, null);

        assertThatThrownBy(() -> fx.service.createFolderNode(10L, 2L, false, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只有科室管理员可以新建最高级文件夹");
    }

    @Test
    void sectionAdminCannotCreateOtherSectionRootFolder() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(3L)).thenReturn(dept(3L, 1L, "技术科", "SECTION"));
        when(fx.orgAssignmentService.adminUserIds()).thenReturn(java.util.Set.of(10L));
        DocNodeRequest request = new DocNodeRequest(3L, null, "年度资料", 20, null, false, "", null, null);

        assertThatThrownBy(() -> fx.service.createFolderNode(10L, 2L, false, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能维护本科室资料");
    }

    @Test
    void superAdminCanCreateAnyRootFolder() {
        Fixtures fx = fixtures();
        when(fx.deptMapper.selectById(3L)).thenReturn(dept(3L, 1L, "技术科", "SECTION"));
        DocNodeRequest request = new DocNodeRequest(3L, null, "年度资料", 20, null, false, "", null, 2030);

        fx.service.createFolderNode(1L, 1L, true, request);

        verify(fx.nodeMapper).insert(any(SysDocNode.class));
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
    void sameUserCannotSubmitNotificationTwice() {
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
                .hasMessageContaining("您已提交过该文件");
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
        SysDocItemWorkshopScopeMapper itemWorkshopScopeMapper = mock(SysDocItemWorkshopScopeMapper.class);
        SysRepairProjectTemplateMapper repairTemplateMapper = mock(SysRepairProjectTemplateMapper.class);
        SysRepairProjectTemplateItemMapper repairTemplateItemMapper = mock(SysRepairProjectTemplateItemMapper.class);
        FileStorageService storageService = mock(FileStorageService.class);
        OrgAssignmentService orgAssignmentService = mock(OrgAssignmentService.class);
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
                itemWorkshopScopeMapper,
                repairTemplateMapper,
                repairTemplateItemMapper,
                storageService,
                orgAssignmentService
        );
        return new Fixtures(deptMapper, userMapper, categoryMapper, itemMapper, nodeMapper, submissionMapper, attachmentMapper, requirementMapper, itemAttachmentMapper, itemWorkshopScopeMapper, repairTemplateMapper, repairTemplateItemMapper, storageService, orgAssignmentService, service);
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

    private SysDocItem item(Long id, Long categoryId, Long sectionDeptId, boolean attachment, String fileType, Integer docYear) {
        SysDocItem item = item(id, categoryId, sectionDeptId, attachment, fileType);
        item.setDocYear(docYear);
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
        node.setDocYear(2026);
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

    private SysDocItemWorkshopScope scope(Long itemId, Long workshopDeptId) {
        SysDocItemWorkshopScope scope = new SysDocItemWorkshopScope();
        scope.setItemId(itemId);
        scope.setWorkshopDeptId(workshopDeptId);
        return scope;
    }

    private SysRepairProjectTemplateItem templateItem(Long templateId, String itemName, Integer sortOrder) {
        SysRepairProjectTemplateItem item = new SysRepairProjectTemplateItem();
        item.setTemplateId(templateId);
        item.setItemName(itemName);
        item.setSortOrder(sortOrder);
        item.setDeleted(0);
        return item;
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
            SysDocItemWorkshopScopeMapper itemWorkshopScopeMapper,
            SysRepairProjectTemplateMapper repairTemplateMapper,
            SysRepairProjectTemplateItemMapper repairTemplateItemMapper,
            FileStorageService storageService,
            OrgAssignmentService orgAssignmentService,
            DocWorkspaceService service
    ) {
    }
}
