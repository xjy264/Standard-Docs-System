package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.entity.SysFileCopyRow;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/copies")
public class CopyController {
    @PostMapping
    public ApiResponse<Void> create(@RequestBody Map<String, Object> body) {
        return copyDisabled();
    }

    @GetMapping("/received")
    public ApiResponse<List<SysFileCopyRow>> received() {
        return copyDisabled();
    }

    @GetMapping("/sent")
    public ApiResponse<List<SysFileCopyRow>> sent() {
        return copyDisabled();
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        return copyDisabled();
    }

    @GetMapping("/read-status")
    public ApiResponse<List<SysFileCopyRow>> readStatus(@RequestParam Long fileId) {
        return copyDisabled();
    }

    private <T> ApiResponse<T> copyDisabled() {
        return ApiResponse.fail(410, "抄送功能已下线");
    }
}
