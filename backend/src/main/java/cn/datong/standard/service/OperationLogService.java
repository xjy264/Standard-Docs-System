package cn.datong.standard.service;

import cn.datong.standard.entity.SysLoginLog;
import cn.datong.standard.entity.SysOperationLog;
import cn.datong.standard.mapper.SysLoginLogMapper;
import cn.datong.standard.mapper.SysOperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OperationLogService {
    private final SysOperationLogMapper operationLogMapper;
    private final SysLoginLogMapper loginLogMapper;

    public void operation(Long operatorId, String operationType, String objectType, Long objectId,
                          String result, String failReason, HttpServletRequest request) {
        SysOperationLog log = new SysOperationLog();
        log.setOperatorId(operatorId);
        log.setOperationType(operationType);
        log.setObjectType(objectType);
        log.setObjectId(objectId);
        log.setResult(result);
        log.setFailReason(failReason);
        log.setIpAddress(clientIp(request));
        log.setUserAgent(request == null ? null : request.getHeader("User-Agent"));
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    public void login(String username, Long userId, String result, String failReason, HttpServletRequest request) {
        SysLoginLog log = new SysLoginLog();
        log.setUsername(username);
        log.setUserId(userId);
        log.setResult(result);
        log.setFailReason(failReason);
        log.setIpAddress(clientIp(request));
        log.setUserAgent(request == null ? null : request.getHeader("User-Agent"));
        log.setCreatedAt(LocalDateTime.now());
        loginLogMapper.insert(log);
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
}
