package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.entity.SysFile;
import cn.datong.standard.entity.SysNotification;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.mapper.SysFileMapper;
import cn.datong.standard.mapper.SysNotificationMapper;
import cn.datong.standard.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final SysUserMapper userMapper;
    private final SysFileMapper fileMapper;
    private final SysNotificationMapper notificationMapper;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Long>> stats() {
        Long userCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeleted, 0));
        Long fileCount = fileMapper.selectCount(new LambdaQueryWrapper<SysFile>().eq(SysFile::getDeleted, 0));
        Long unreadCount = notificationMapper.selectCount(new LambdaQueryWrapper<SysNotification>().eq(SysNotification::getReadStatus, "UNREAD"));
        return ApiResponse.success(Map.of("userCount", userCount, "fileCount", fileCount, "unreadCount", unreadCount));
    }
}
