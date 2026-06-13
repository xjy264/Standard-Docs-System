package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.SectionFileTreeItem;
import cn.datong.standard.entity.SysDept;
import cn.datong.standard.entity.SysDocCategory;
import cn.datong.standard.entity.SysDocItem;
import cn.datong.standard.mapper.SysDeptMapper;
import cn.datong.standard.mapper.SysDocAttachmentMapper;
import cn.datong.standard.mapper.SysDocCategoryMapper;
import cn.datong.standard.mapper.SysDocItemMapper;
import cn.datong.standard.mapper.SysDocSubmissionMapper;
import cn.datong.standard.mapper.SysNotificationMapper;
import cn.datong.standard.mapper.SysRegisterApprovalMapper;
import cn.datong.standard.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardControllerTest {

    @Test
    void statsReturnsHomepageCounts() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysDocItemMapper docItemMapper = mock(SysDocItemMapper.class);
        SysNotificationMapper notificationMapper = mock(SysNotificationMapper.class);
        SysDocSubmissionMapper submissionMapper = mock(SysDocSubmissionMapper.class);
        SysDocAttachmentMapper attachmentMapper = mock(SysDocAttachmentMapper.class);
        SysRegisterApprovalMapper approvalMapper = mock(SysRegisterApprovalMapper.class);
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysDocCategoryMapper categoryMapper = mock(SysDocCategoryMapper.class);
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(11L);
        when(docItemMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(23L, 7L);
        when(notificationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);
        when(submissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(13L);
        when(attachmentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(31L);
        when(approvalMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(deptMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(9L);
        DashboardController controller = new DashboardController(
                userMapper,
                docItemMapper,
                notificationMapper,
                submissionMapper,
                attachmentMapper,
                approvalMapper,
                deptMapper,
                categoryMapper
        );

        ApiResponse<Map<String, Long>> response = controller.stats();

        assertThat(response.getData()).containsEntry("userCount", 11L)
                .containsEntry("fileCount", 23L)
                .containsEntry("submissionCount", 13L)
                .containsEntry("attachmentCount", 31L)
                .containsEntry("pendingApprovalCount", 2L)
                .containsEntry("unreadCount", 5L)
                .containsEntry("uploadEnabledFileCount", 7L)
                .containsEntry("sectionCount", 9L);
    }

    @Test
    void sectionFileTreeReturnsDeptTreeWithFileCounts() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysDocItemMapper docItemMapper = mock(SysDocItemMapper.class);
        SysNotificationMapper notificationMapper = mock(SysNotificationMapper.class);
        SysDocSubmissionMapper submissionMapper = mock(SysDocSubmissionMapper.class);
        SysDocAttachmentMapper attachmentMapper = mock(SysDocAttachmentMapper.class);
        SysRegisterApprovalMapper approvalMapper = mock(SysRegisterApprovalMapper.class);
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysDocCategoryMapper categoryMapper = mock(SysDocCategoryMapper.class);
        when(deptMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                dept(1L, 0L, "机关", "AGENCY"),
                dept(2L, 1L, "技术科", "SECTION")
        ));
        when(categoryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(category(6L, 2L)));
        when(docItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                item(8L, 6L, null),
                item(9L, null, 2L)
        ));
        DashboardController controller = new DashboardController(
                userMapper,
                docItemMapper,
                notificationMapper,
                submissionMapper,
                attachmentMapper,
                approvalMapper,
                deptMapper,
                categoryMapper
        );

        ApiResponse<List<SectionFileTreeItem>> response = controller.sectionFileTree();

        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().getFirst().fileCount()).isEqualTo(2L);
        assertThat(response.getData().getFirst().children().getFirst().deptName()).isEqualTo("技术科");
        assertThat(response.getData().getFirst().children().getFirst().fileCount()).isEqualTo(2L);
    }

    private SysDept dept(Long id, Long parentId, String name, String type) {
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setParentId(parentId);
        dept.setDeptName(name);
        dept.setDeptType(type);
        dept.setSortOrder(Math.toIntExact(id));
        dept.setStatus("ENABLED");
        dept.setDeleted(0);
        return dept;
    }

    private SysDocCategory category(Long id, Long sectionDeptId) {
        SysDocCategory category = new SysDocCategory();
        category.setId(id);
        category.setSectionDeptId(sectionDeptId);
        category.setDeleted(0);
        return category;
    }

    private SysDocItem item(Long id, Long categoryId, Long sectionDeptId) {
        SysDocItem item = new SysDocItem();
        item.setId(id);
        item.setCategoryId(categoryId);
        item.setSectionDeptId(sectionDeptId);
        item.setDeleted(0);
        return item;
    }
}
