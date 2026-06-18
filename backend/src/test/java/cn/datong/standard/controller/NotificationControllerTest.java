package cn.datong.standard.controller;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.entity.SysNotification;
import cn.datong.standard.mapper.SysNotificationMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationControllerTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readRejectsNotificationsOwnedByAnotherUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new CurrentUser(1L, 1L, false), null, List.of())
        );
        SysNotificationMapper notificationMapper = mock(SysNotificationMapper.class);
        SysNotification notification = new SysNotification();
        notification.setId(8L);
        notification.setUserId(2L);
        when(notificationMapper.selectById(8L)).thenReturn(notification);
        NotificationController controller = new NotificationController(notificationMapper);

        assertThatThrownBy(() -> controller.read(8L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("通知不存在");

        verify(notificationMapper, never()).updateById(notification);
    }
}
