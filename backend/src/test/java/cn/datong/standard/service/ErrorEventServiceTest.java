package cn.datong.standard.service;

import cn.datong.standard.entity.SysErrorEvent;
import cn.datong.standard.mapper.SysErrorEventMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErrorEventServiceTest {
    @Test
    void recordBackendExceptionStoresTraceAndRedactsSensitiveHeaders() {
        SysErrorEventMapper mapper = mock(SysErrorEventMapper.class);
        ErrorEventService service = new ErrorEventService(mapper);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/doc-tree");
        when(request.getQueryString()).thenReturn("password=secret&keyword=abc");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getAttribute("TRACE_ID")).thenReturn("trace-123");

        SysErrorEvent event = service.recordBackendException(
                new IllegalStateException("database password=secret token=abc123"),
                request,
                500,
                500,
                "系统服务暂时不可用",
                9L,
                24L
        );

        ArgumentCaptor<SysErrorEvent> captor = ArgumentCaptor.forClass(SysErrorEvent.class);
        verify(mapper).insert(captor.capture());
        SysErrorEvent saved = captor.getValue();
        assertThat(event.getErrorId()).isEqualTo(saved.getErrorId());
        assertThat(saved.getTraceId()).isEqualTo("trace-123");
        assertThat(saved.getSource()).isEqualTo("BACKEND");
        assertThat(saved.getRequestUri()).isEqualTo("/api/doc-tree");
        assertThat(saved.getQueryString()).contains("password=[REDACTED]");
        assertThat(saved.getMessage()).contains("password=[REDACTED]");
        assertThat(saved.getMessage()).doesNotContain("secret");
        assertThat(saved.getStackTrace()).contains("IllegalStateException");
        assertThat(saved.getUserId()).isEqualTo(9L);
        assertThat(saved.getDeptId()).isEqualTo(24L);
        assertThat(saved.getFingerprint()).isNotBlank();
    }
}
