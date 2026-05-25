package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.mapper.SysDeptMapper;
import cn.datong.standard.mapper.SysDocAttachmentMapper;
import cn.datong.standard.mapper.SysDocItemMapper;
import cn.datong.standard.mapper.SysDocSubmissionMapper;
import cn.datong.standard.mapper.SysNotificationMapper;
import cn.datong.standard.mapper.SysRegisterApprovalMapper;
import cn.datong.standard.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;

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
                deptMapper
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
}
