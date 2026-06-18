package cn.datong.standard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_error_event")
public class SysErrorEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String errorId;
    private String traceId;
    private String source;
    private String severity;
    private String fingerprint;
    private Integer statusCode;
    private Integer businessCode;
    private String message;
    private String exceptionClass;
    private String stackTrace;
    private String requestMethod;
    private String requestUri;
    private String queryString;
    private String requestBodyDigest;
    private Long userId;
    private Long deptId;
    private String ipAddress;
    private String userAgent;
    private String frontendRoute;
    private String frontendComponent;
    private String frontendStack;
    private String browserInfo;
    private String releaseVersion;
    private String gitCommit;
    private String serverName;
    private Boolean resolved;
    private LocalDateTime resolvedAt;
    private Long resolvedBy;
    private String remark;
    private LocalDateTime createdAt;
}
