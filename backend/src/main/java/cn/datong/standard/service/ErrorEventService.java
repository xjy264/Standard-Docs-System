package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.dto.ErrorEventListResponse;
import cn.datong.standard.dto.ErrorEventStat;
import cn.datong.standard.entity.SysErrorEvent;
import cn.datong.standard.mapper.SysErrorEventMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorEventService {
    public static final String TRACE_ID_ATTRIBUTE = "TRACE_ID";
    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter ERROR_ID_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final List<String> SENSITIVE_KEYS = List.of(
            "password", "passwd", "pwd", "token", "authorization", "cookie", "secret",
            "jwt", "csrf", "access_key", "secret_key", "minio", "credential"
    );

    private final SysErrorEventMapper errorEventMapper;

    @Value("${app.version:0.1.0}")
    private String releaseVersion;
    @Value("${app.git-commit:local}")
    private String gitCommit;

    public SysErrorEvent recordBackendException(Exception ex, HttpServletRequest request, int statusCode,
                                                int businessCode, String userMessage, Long userId, Long deptId) {
        SysErrorEvent event = new SysErrorEvent();
        event.setErrorId(newErrorId());
        event.setTraceId(traceId(request));
        event.setSource("BACKEND");
        event.setSeverity(statusCode >= 500 ? "ERROR" : "WARN");
        event.setStatusCode(statusCode);
        event.setBusinessCode(businessCode);
        event.setMessage(limit(redact(ex.getMessage() == null ? userMessage : ex.getMessage()), 1000));
        event.setExceptionClass(ex.getClass().getName());
        event.setStackTrace(limit(stackTrace(ex), 60000));
        event.setRequestMethod(request == null ? null : request.getMethod());
        event.setRequestUri(request == null ? null : request.getRequestURI());
        event.setQueryString(limit(redact(request == null ? null : request.getQueryString()), 1000));
        event.setUserId(userId);
        event.setDeptId(deptId);
        event.setIpAddress(clientIp(request));
        event.setUserAgent(limit(request == null ? null : request.getHeader("User-Agent"), 500));
        fillRuntime(event);
        event.setResolved(false);
        event.setCreatedAt(LocalDateTime.now());
        event.setFingerprint(fingerprint(String.join("|",
                event.getSource(),
                safe(event.getExceptionClass()),
                safe(event.getRequestUri()),
                firstStackLine(event.getStackTrace())
        )));
        insertQuietly(event);
        return event;
    }

    public SysErrorEvent recordFrontendEvent(FrontendErrorReport report, CurrentUser currentUser, HttpServletRequest request) {
        SysErrorEvent event = new SysErrorEvent();
        event.setErrorId(newErrorId());
        event.setTraceId(limit(redact(report.traceId()), 64));
        event.setSource("FRONTEND");
        event.setSeverity("ERROR");
        event.setStatusCode(report.statusCode());
        event.setBusinessCode(report.businessCode());
        event.setMessage(limit(redact(report.message()), 1000));
        event.setExceptionClass(limit(redact(report.errorType()), 255));
        event.setRequestMethod(limit(report.method(), 16));
        event.setRequestUri(limit(redact(report.url()), 500));
        event.setUserId(currentUser == null ? null : currentUser.userId());
        event.setDeptId(currentUser == null ? null : currentUser.deptId());
        event.setIpAddress(clientIp(request));
        event.setUserAgent(limit(request == null ? null : request.getHeader("User-Agent"), 500));
        event.setFrontendRoute(limit(redact(report.route()), 500));
        event.setFrontendComponent(limit(redact(report.component()), 255));
        event.setFrontendStack(limit(redact(report.stack()), 60000));
        event.setBrowserInfo(limit(redact(report.browserInfo()), 1000));
        fillRuntime(event);
        event.setResolved(false);
        event.setCreatedAt(LocalDateTime.now());
        event.setFingerprint(fingerprint(String.join("|",
                event.getSource(),
                safe(event.getExceptionClass()),
                safe(event.getFrontendRoute()),
                safe(event.getMessage())
        )));
        insertQuietly(event);
        return event;
    }

    public ErrorEventListResponse list(String source, String severity, Boolean resolved, String keyword,
                                       LocalDateTime startTime, LocalDateTime endTime, Integer page, Integer size) {
        int pageNumber = Math.max(page == null ? 1 : page, 1);
        int pageSize = Math.min(Math.max(size == null ? 20 : size, 1), MAX_PAGE_SIZE);
        LambdaQueryWrapper<SysErrorEvent> base = wrapper(source, severity, resolved, keyword, startTime, endTime);
        long total = errorEventMapper.selectCount(base);
        LambdaQueryWrapper<SysErrorEvent> rowsWrapper = wrapper(source, severity, resolved, keyword, startTime, endTime)
                .orderByDesc(SysErrorEvent::getCreatedAt)
                .last("LIMIT " + pageSize + " OFFSET " + ((pageNumber - 1) * pageSize));
        List<SysErrorEvent> rows = errorEventMapper.selectList(rowsWrapper);
        return new ErrorEventListResponse(rows, total, stats(source, severity, resolved, keyword, startTime, endTime));
    }

    public SysErrorEvent detail(Long id) {
        SysErrorEvent event = errorEventMapper.selectById(id);
        if (event == null) {
            throw new BusinessException(404, "错误记录不存在");
        }
        return event;
    }

    public void resolve(Long id, Long operatorId, String remark) {
        SysErrorEvent event = detail(id);
        event.setResolved(true);
        event.setResolvedAt(LocalDateTime.now());
        event.setResolvedBy(operatorId);
        event.setRemark(limit(redact(remark), 1000));
        errorEventMapper.updateById(event);
    }

    public byte[] exportZip(String source, String severity, Boolean resolved, Integer days) {
        LocalDateTime start = days == null || days <= 0 ? null : LocalDateTime.now().minusDays(Math.min(days, 365));
        List<SysErrorEvent> events = errorEventMapper.selectList(wrapper(source, severity, resolved, null, start, null)
                .orderByDesc(SysErrorEvent::getCreatedAt)
                .last("LIMIT 10000"));
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
                writeZip(zip, "error-events.csv", csv(events).getBytes(StandardCharsets.UTF_8));
                writeZip(zip, "error-events.json", json(events).getBytes(StandardCharsets.UTF_8));
                writeZip(zip, "system-info.json", systemInfo(events.size()).getBytes(StandardCharsets.UTF_8));
                writeZip(zip, "doctor.txt", doctorText().getBytes(StandardCharsets.UTF_8));
                writeZip(zip, "README.txt", readmeText().getBytes(StandardCharsets.UTF_8));
            }
            return output.toByteArray();
        } catch (Exception ex) {
            throw new BusinessException(500, "错误记录导出失败，请稍后重试。");
        }
    }

    private List<ErrorEventStat> stats(String source, String severity, Boolean resolved, String keyword,
                                       LocalDateTime startTime, LocalDateTime endTime) {
        List<SysErrorEvent> events = errorEventMapper.selectList(wrapper(source, severity, resolved, keyword, startTime, endTime)
                .orderByDesc(SysErrorEvent::getCreatedAt)
                .last("LIMIT 1000"));
        Map<String, ErrorEventStatBuilder> grouped = new LinkedHashMap<>();
        for (SysErrorEvent event : events) {
            String key = safe(event.getFingerprint());
            grouped.computeIfAbsent(key, ignored -> new ErrorEventStatBuilder(event)).increment();
        }
        return grouped.values().stream()
                .sorted(Comparator.comparingLong(ErrorEventStatBuilder::count).reversed())
                .limit(20)
                .map(ErrorEventStatBuilder::toStat)
                .toList();
    }

    private LambdaQueryWrapper<SysErrorEvent> wrapper(String source, String severity, Boolean resolved, String keyword,
                                                     LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<SysErrorEvent> wrapper = new LambdaQueryWrapper<>();
        if (source != null && !source.isBlank()) {
            wrapper.eq(SysErrorEvent::getSource, source.trim().toUpperCase(Locale.ROOT));
        }
        if (severity != null && !severity.isBlank()) {
            wrapper.eq(SysErrorEvent::getSeverity, severity.trim().toUpperCase(Locale.ROOT));
        }
        if (resolved != null) {
            wrapper.eq(SysErrorEvent::getResolved, resolved);
        }
        if (startTime != null) {
            wrapper.ge(SysErrorEvent::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(SysErrorEvent::getCreatedAt, endTime);
        }
        if (keyword != null && !keyword.isBlank()) {
            String text = keyword.trim();
            wrapper.and(w -> w.like(SysErrorEvent::getErrorId, text)
                    .or().like(SysErrorEvent::getTraceId, text)
                    .or().like(SysErrorEvent::getMessage, text)
                    .or().like(SysErrorEvent::getRequestUri, text)
                    .or().like(SysErrorEvent::getFrontendRoute, text));
        }
        return wrapper;
    }

    private void insertQuietly(SysErrorEvent event) {
        try {
            errorEventMapper.insert(event);
        } catch (Exception ex) {
            log.error("错误事件落库失败，errorId={}", event.getErrorId(), ex);
        }
    }

    private void fillRuntime(SysErrorEvent event) {
        event.setReleaseVersion(limit(releaseVersion, 64));
        event.setGitCommit(limit(gitCommit, 64));
        event.setServerName(limit(System.getenv().getOrDefault("HOSTNAME", "local"), 128));
    }

    private String newErrorId() {
        return "ERR-" + LocalDateTime.now().format(ERROR_ID_TIME) + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String traceId(HttpServletRequest request) {
        Object value = request == null ? null : request.getAttribute(TRACE_ID_ATTRIBUTE);
        return value == null ? null : String.valueOf(value);
    }

    private String stackTrace(Throwable ex) {
        StringWriter writer = new StringWriter();
        ex.printStackTrace(new PrintWriter(writer));
        return redact(writer.toString());
    }

    private String firstStackLine(String stackTrace) {
        if (stackTrace == null || stackTrace.isBlank()) {
            return "";
        }
        String[] lines = stackTrace.split("\\R");
        return lines.length > 1 ? lines[1].trim() : lines[0].trim();
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String redact(String value) {
        if (value == null) {
            return null;
        }
        String redacted = value;
        for (String key : SENSITIVE_KEYS) {
            redacted = redacted.replaceAll("(?i)(" + key + "\\s*[=:]\\s*)[^&\\s,;]+", "$1[REDACTED]");
        }
        return redacted;
    }

    private String fingerprint(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                builder.append(String.format("%02x", hash[i]));
            }
            return builder.toString();
        } catch (Exception ex) {
            return UUID.randomUUID().toString().substring(0, 8);
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void writeZip(ZipOutputStream zip, String name, byte[] bytes) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }

    private String csv(List<SysErrorEvent> events) {
        StringBuilder builder = new StringBuilder("\uFEFF错误编号,追踪编号,来源,级别,状态码,业务码,消息,异常类,请求地址,前端路由,用户ID,部门ID,是否处理,创建时间\n");
        for (SysErrorEvent event : events) {
            builder.append(csvCell(event.getErrorId())).append(',')
                    .append(csvCell(event.getTraceId())).append(',')
                    .append(csvCell(event.getSource())).append(',')
                    .append(csvCell(event.getSeverity())).append(',')
                    .append(csvCell(event.getStatusCode())).append(',')
                    .append(csvCell(event.getBusinessCode())).append(',')
                    .append(csvCell(event.getMessage())).append(',')
                    .append(csvCell(event.getExceptionClass())).append(',')
                    .append(csvCell(event.getRequestUri())).append(',')
                    .append(csvCell(event.getFrontendRoute())).append(',')
                    .append(csvCell(event.getUserId())).append(',')
                    .append(csvCell(event.getDeptId())).append(',')
                    .append(csvCell(Boolean.TRUE.equals(event.getResolved()) ? "是" : "否")).append(',')
                    .append(csvCell(event.getCreatedAt())).append('\n');
        }
        return builder.toString();
    }

    private String csvCell(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private String json(List<SysErrorEvent> events) {
        StringBuilder builder = new StringBuilder("[\n");
        List<String> rows = new ArrayList<>();
        for (SysErrorEvent event : events) {
            rows.add("  {\"id\":" + event.getId()
                    + ",\"errorId\":" + jsonString(event.getErrorId())
                    + ",\"traceId\":" + jsonString(event.getTraceId())
                    + ",\"source\":" + jsonString(event.getSource())
                    + ",\"severity\":" + jsonString(event.getSeverity())
                    + ",\"message\":" + jsonString(event.getMessage())
                    + ",\"exceptionClass\":" + jsonString(event.getExceptionClass())
                    + ",\"requestUri\":" + jsonString(event.getRequestUri())
                    + ",\"frontendRoute\":" + jsonString(event.getFrontendRoute())
                    + ",\"stackTrace\":" + jsonString(event.getStackTrace())
                    + ",\"frontendStack\":" + jsonString(event.getFrontendStack())
                    + ",\"createdAt\":" + jsonString(event.getCreatedAt())
                    + "}");
        }
        builder.append(String.join(",\n", rows));
        builder.append("\n]\n");
        return builder.toString();
    }

    private String systemInfo(int count) {
        return "{\n"
                + "  \"generatedAt\": " + jsonString(LocalDateTime.now().format(FILE_TIME)) + ",\n"
                + "  \"releaseVersion\": " + jsonString(releaseVersion) + ",\n"
                + "  \"gitCommit\": " + jsonString(gitCommit) + ",\n"
                + "  \"serverName\": " + jsonString(System.getenv().getOrDefault("HOSTNAME", "local")) + ",\n"
                + "  \"eventCount\": " + count + "\n"
                + "}\n";
    }

    private String doctorText() {
        return "请在服务器执行 ./run.sh doctor 获取实时自检结果。本导出包包含应用侧错误事件和生成时的版本信息。\n";
    }

    private String readmeText() {
        return "本故障包由标准化资料管理系统生成。error-events.csv 适合用表格查看，error-events.json 包含完整堆栈。敏感字段已脱敏。\n";
    }

    private String jsonString(Object value) {
        if (value == null) {
            return "null";
        }
        String text = String.valueOf(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
        return "\"" + text + "\"";
    }

    private static class ErrorEventStatBuilder {
        private final String fingerprint;
        private final String source;
        private final String severity;
        private final String message;
        private long count;

        ErrorEventStatBuilder(SysErrorEvent event) {
            this.fingerprint = event.getFingerprint();
            this.source = event.getSource();
            this.severity = event.getSeverity();
            this.message = event.getMessage();
        }

        void increment() {
            count++;
        }

        long count() {
            return count;
        }

        ErrorEventStat toStat() {
            return new ErrorEventStat(fingerprint, source, severity, message, count);
        }
    }

    public record FrontendErrorReport(
            String message,
            String errorType,
            String stack,
            String route,
            String component,
            String browserInfo,
            String traceId,
            Integer statusCode,
            Integer businessCode,
            String method,
            String url
    ) {
    }
}
