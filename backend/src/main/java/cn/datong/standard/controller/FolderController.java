package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.entity.SysFolder;
import cn.datong.standard.mapper.SysFolderMapper;
import cn.datong.standard.security.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {
    private final SysFolderMapper folderMapper;

    @GetMapping
    public ApiResponse<List<SysFolder>> list() {
        return ApiResponse.success(folderMapper.selectList(new LambdaQueryWrapper<SysFolder>()
                .eq(SysFolder::getDeleted, 0)
                .orderByAsc(SysFolder::getSortOrder)));
    }

    @PostMapping
    public ApiResponse<SysFolder> create(@RequestBody SysFolder folder) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        folder.setOwnerUserId(currentUser.userId());
        folder.setDeptId(currentUser.deptId());
        folder.setCreatedAt(LocalDateTime.now());
        folder.setUpdatedAt(LocalDateTime.now());
        folder.setDeleted(0);
        folderMapper.insert(folder);
        return ApiResponse.success(folder);
    }

    @PutMapping("/{id}")
    public ApiResponse<SysFolder> update(@PathVariable Long id, @RequestBody SysFolder folder) {
        folder.setId(id);
        folder.setUpdatedAt(LocalDateTime.now());
        folderMapper.updateById(folder);
        return ApiResponse.success(folderMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        folderMapper.deleteById(id);
        return ApiResponse.success();
    }
}
