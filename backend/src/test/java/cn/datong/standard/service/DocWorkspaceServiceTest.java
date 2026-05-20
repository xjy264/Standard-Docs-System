package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.entity.SysDept;
import cn.datong.standard.entity.SysDocCategory;
import cn.datong.standard.entity.SysDocItem;
import cn.datong.standard.entity.SysDocSubmission;
import cn.datong.standard.mapper.SysDeptMapper;
import cn.datong.standard.mapper.SysDocAttachmentMapper;
import cn.datong.standard.mapper.SysDocCategoryMapper;
import cn.datong.standard.mapper.SysDocItemMapper;
import cn.datong.standard.mapper.SysDocSubmissionMapper;
import cn.datong.standard.mapper.SysUserMapper;
import cn.datong.standard.storage.FileStorageService;
import cn.datong.standard.storage.StoredObject;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
                .hasMessageContaining("该文件未开启附件上传");
    }

    @Test
    void workshopSubmissionUploadsAttachmentWhenEnabled() {
        Fixtures fx = fixtures();
        when(fx.itemMapper.selectById(8L)).thenReturn(item(8L, 6L, true));
        when(fx.categoryMapper.selectById(6L)).thenReturn(category(6L, 2L));
        when(fx.deptMapper.selectById(5L)).thenReturn(dept(5L, 0L, "房建车间", "WORKSHOP"));
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

        fx.service.submit(20L, 5L, 8L, null, List.of(file, image));

        verify(fx.attachmentMapper, times(2)).insert(any(cn.datong.standard.entity.SysDocAttachment.class));
    }

    @Test
    void sectionUserCanUploadOwnSectionAttachment() {
        Fixtures fx = fixtures();
        when(fx.itemMapper.selectById(8L)).thenReturn(item(8L, 6L, true));
        when(fx.categoryMapper.selectById(6L)).thenReturn(category(6L, 2L));
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
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
    void sectionUserCanUploadOtherSectionAttachment() {
        Fixtures fx = fixtures();
        when(fx.itemMapper.selectById(8L)).thenReturn(item(8L, 6L, true));
        when(fx.categoryMapper.selectById(6L)).thenReturn(category(6L, 3L));
        when(fx.deptMapper.selectById(2L)).thenReturn(dept(2L, 1L, "办公室", "SECTION"));
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
    void itemSubmissionQueryFiltersWorkshopRecords() {
        Fixtures fx = fixtures();
        when(fx.itemMapper.selectById(8L)).thenReturn(item(8L, 6L, true));
        when(fx.categoryMapper.selectById(6L)).thenReturn(category(6L, 2L));
        when(fx.deptMapper.selectById(5L)).thenReturn(dept(5L, 0L, "房建车间", "WORKSHOP"));
        SysDocSubmission own = new SysDocSubmission();
        own.setId(1L);
        own.setItemId(8L);
        own.setCategoryId(6L);
        own.setWorkshopDeptId(5L);
        own.setSubmitterDeptId(5L);
        when(fx.submissionMapper.selectList(any())).thenReturn(List.of(own));
        when(fx.deptMapper.selectList(any())).thenReturn(List.of());
        when(fx.userMapper.selectList(any())).thenReturn(List.of());
        when(fx.categoryMapper.selectList(any())).thenReturn(List.of());
        when(fx.itemMapper.selectList(any())).thenReturn(List.of());
        when(fx.attachmentMapper.selectCount(any())).thenReturn(0L);

        assertThat(fx.service.itemSubmissions(20L, 5L, false, 8L)).containsExactly(own);
    }

    private Fixtures fixtures() {
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysDocCategoryMapper categoryMapper = mock(SysDocCategoryMapper.class);
        SysDocItemMapper itemMapper = mock(SysDocItemMapper.class);
        SysDocSubmissionMapper submissionMapper = mock(SysDocSubmissionMapper.class);
        SysDocAttachmentMapper attachmentMapper = mock(SysDocAttachmentMapper.class);
        FileStorageService storageService = mock(FileStorageService.class);
        DocWorkspaceService service = new DocWorkspaceService(
                deptMapper,
                userMapper,
                categoryMapper,
                itemMapper,
                submissionMapper,
                attachmentMapper,
                storageService
        );
        return new Fixtures(deptMapper, userMapper, categoryMapper, itemMapper, submissionMapper, attachmentMapper, storageService, service);
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
            SysDocSubmissionMapper submissionMapper,
            SysDocAttachmentMapper attachmentMapper,
            FileStorageService storageService,
            DocWorkspaceService service
    ) {
    }
}
