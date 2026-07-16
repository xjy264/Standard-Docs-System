package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.DeptNavigationItem;
import cn.datong.standard.service.DocWorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sections")
@RequiredArgsConstructor
public class SectionController {
    private final DocWorkspaceService docWorkspaceService;

    @GetMapping("/navigation")
    public ApiResponse<List<DeptNavigationItem>> navigation(@RequestParam(required = false) String moduleType) {
        return ApiResponse.success(docWorkspaceService.sections(moduleType));
    }
}
