package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {
    @Value("${app.version:0.1.0}")
    private String version;

    @Value("${app.git-commit:local}")
    private String gitCommit;

    @GetMapping("/version")
    public ApiResponse<Map<String, Object>> version() {
        return ApiResponse.success(Map.of(
                "version", version,
                "gitCommit", gitCommit,
                "serverTime", LocalDateTime.now()
        ));
    }
}
