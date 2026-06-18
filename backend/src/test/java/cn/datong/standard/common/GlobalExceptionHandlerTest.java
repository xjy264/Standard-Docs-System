package cn.datong.standard.common;

import cn.datong.standard.entity.SysErrorEvent;
import cn.datong.standard.service.ErrorEventService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {
    @Test
    void unhandledExceptionReturnsUserSafeMessageWithErrorId() {
        ErrorEventService errorEventService = mock(ErrorEventService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        SysErrorEvent event = new SysErrorEvent();
        event.setErrorId("ERR-20260618-ABC123");
        when(errorEventService.recordBackendException(
                org.mockito.ArgumentMatchers.any(Exception.class),
                eq(request),
                eq(500),
                eq(500),
                eq("系统服务暂时不可用，请稍后重试或联系管理员。"),
                eq(null),
                eq(null)
        )).thenReturn(event);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(errorEventService);
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        ApiResponse<Void> response = handler.handleException(new IllegalStateException("SQL password=secret"), request, servletResponse);

        assertThat(servletResponse.getStatus()).isEqualTo(500);
        assertThat(response.getCode()).isEqualTo(500);
        assertThat(response.getMessage()).contains("系统服务暂时不可用");
        assertThat(response.getMessage()).contains("错误编号：ERR-20260618-ABC123");
        assertThat(response.getMessage()).doesNotContain("SQL");
        assertThat(response.getMessage()).doesNotContain("secret");
    }

    @Test
    void businessExceptionSetsHttpStatusFromBusinessCode() {
        ErrorEventService errorEventService = mock(ErrorEventService.class);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(errorEventService);
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        ApiResponse<Void> response = handler.handleBusiness(
                new BusinessException(403, "只有超级管理员可以查看系统错误。"),
                mock(HttpServletRequest.class),
                servletResponse
        );

        assertThat(servletResponse.getStatus()).isEqualTo(403);
        assertThat(response.getCode()).isEqualTo(403);
    }
}
