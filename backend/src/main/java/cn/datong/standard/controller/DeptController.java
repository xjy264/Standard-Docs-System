package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.entity.SysDept;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.mapper.SysDeptMapper;
import cn.datong.standard.mapper.SysUserMapper;
import cn.datong.standard.security.SecurityUtils;
import cn.datong.standard.service.DeptAdminCoverage;
import cn.datong.standard.service.DeptDetail;
import cn.datong.standard.service.OrgAssignmentService;
import cn.datong.standard.service.PermissionService;
import cn.datong.standard.service.UserAdminService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/depts")
@RequiredArgsConstructor
public class DeptController {
    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;
    private final PermissionService permissionService;
    private final UserAdminService userAdminService;
    private final OrgAssignmentService orgAssignmentService;

    @GetMapping("/tree")
    public ApiResponse<List<SysDept>> tree() {
        return ApiResponse.success(deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeleted, 0)
                .orderByAsc(SysDept::getSortOrder)));
    }

    @GetMapping("/assignable")
    public ApiResponse<List<SysDept>> assignable() {
        return ApiResponse.success(orgAssignmentService.assignableDepts());
    }

    @GetMapping("/admin-coverage")
    public ApiResponse<List<DeptAdminCoverage>> adminCoverage() {
        CurrentUser currentUser = SecurityUtils.currentUser();
        return ApiResponse.success(userAdminService.adminCoverage(currentUser.superAdmin()));
    }

    @GetMapping("/{id}/detail")
    public ApiResponse<DeptDetail> detail(@PathVariable Long id) {
        return ApiResponse.success(orgAssignmentService.detail(id));
    }

    @PostMapping
    public ApiResponse<SysDept> create(@RequestBody SysDept dept, HttpServletRequest request) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        requireSuperAdmin(currentUser);
        dept.setCreatedAt(LocalDateTime.now());
        dept.setUpdatedAt(LocalDateTime.now());
        dept.setDeleted(0);
        deptMapper.insert(dept);
        return ApiResponse.success(dept);
    }

    @PutMapping("/{id}")
    public ApiResponse<SysDept> update(@PathVariable Long id, @RequestBody SysDept dept) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        requireSuperAdmin(currentUser);
        dept.setId(id);
        dept.setUpdatedAt(LocalDateTime.now());
        deptMapper.updateById(dept);
        return ApiResponse.success(deptMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        requireSuperAdmin(currentUser);
        deptMapper.deleteById(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/users")
    public ApiResponse<List<SysUser>> users(@PathVariable Long id) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        permissionService.require(currentUser.userId(), currentUser.superAdmin(), "user:view");
        return ApiResponse.success(userMapper.selectList(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, id)));
    }

    private void requireSuperAdmin(CurrentUser currentUser) {
        if (!currentUser.superAdmin()) {
            throw new cn.datong.standard.common.BusinessException(403, "只有超级管理员可以管理组织");
        }
    }
}
