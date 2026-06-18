package cn.datong.standard.controller;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.service.ErrorEventService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ErrorEventControllerTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void nonSuperAdminCannotListResolveOrExportErrorEvents() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new CurrentUser(2L, 8L, false), null, List.of())
        );
        ErrorEventService service = mock(ErrorEventService.class);
        ErrorEventController controller = new ErrorEventController(service);

        assertThatThrownBy(() -> controller.list(null, null, null, null, null, null, 1, 20))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只有超级管理员可以查看系统错误。");
        assertThatThrownBy(() -> controller.detail(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只有超级管理员可以查看系统错误。");
        assertThatThrownBy(() -> controller.resolve(1L, new ErrorEventController.ResolveRequest("已处理")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只有超级管理员可以查看系统错误。");
        assertThatThrownBy(() -> controller.export(null, null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只有超级管理员可以查看系统错误。");

        verify(service, never()).list(null, null, null, null, null, null, 1, 20);
        verify(service, never()).resolve(1L, 2L, "已处理");
    }

    @Test
    void superAdminCanListErrorEvents() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new CurrentUser(1L, 1L, true), null, List.of())
        );
        ErrorEventService service = mock(ErrorEventService.class);
        ErrorEventController controller = new ErrorEventController(service);

        controller.list("BACKEND", "ERROR", null, null, null, null, 1, 20);

        verify(service).list("BACKEND", "ERROR", null, null, null, null, 1, 20);
    }
}
