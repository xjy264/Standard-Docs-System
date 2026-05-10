package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.entity.SysNotification;
import cn.datong.standard.mapper.SysNotificationMapper;
import cn.datong.standard.security.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final SysNotificationMapper notificationMapper;

    @GetMapping
    public ApiResponse<List<SysNotification>> list() {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(notificationMapper.selectList(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, currentUser.userId())
                .orderByDesc(SysNotification::getCreatedAt)));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount() {
        CurrentUser currentUser = SecurityUtils.currentUser();
        Long count = notificationMapper.selectCount(new LambdaQueryWrapper<SysNotification>()
                .eq(SysNotification::getUserId, currentUser.userId())
                .eq(SysNotification::getReadStatus, "UNREAD"));
        return ApiResponse.success(Map.of("count", count));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> read(@PathVariable Long id) {
        SysNotification notification = notificationMapper.selectById(id);
        notification.setReadStatus("READ");
        notification.setReadTime(LocalDateTime.now());
        notificationMapper.updateById(notification);
        return ApiResponse.success();
    }
}
