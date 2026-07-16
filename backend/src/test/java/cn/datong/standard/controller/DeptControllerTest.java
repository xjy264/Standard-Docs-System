package cn.datong.standard.controller;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.entity.SysDept;
import cn.datong.standard.mapper.SysDeptMapper;
import cn.datong.standard.mapper.SysUserMapper;
import cn.datong.standard.service.DeptNavigationService;
import cn.datong.standard.service.DeptOverviewService;
import cn.datong.standard.service.OrgAssignmentService;
import cn.datong.standard.service.PermissionService;
import cn.datong.standard.service.UserAdminService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeptControllerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateRejectsFixedNavigationDepartment() {
        authenticateAsSuperAdmin();
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        when(deptMapper.selectById(2L)).thenReturn(dept(2L, "OFFICE"));
        DeptController controller = controller(deptMapper);

        assertThatThrownBy(() -> controller.update(2L, dept(2L, "OFFICE")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("固定资料侧边栏不允许修改");

        verify(deptMapper, never()).updateById(any(SysDept.class));
    }

    @Test
    void deleteRejectsFixedNavigationDepartment() {
        authenticateAsSuperAdmin();
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        when(deptMapper.selectById(2L)).thenReturn(dept(2L, "OFFICE"));
        DeptController controller = controller(deptMapper);

        assertThatThrownBy(() -> controller.delete(2L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("固定资料侧边栏不允许修改");

        verify(deptMapper, never()).deleteById(2L);
    }

    private DeptController controller(SysDeptMapper deptMapper) {
        return new DeptController(
                deptMapper,
                mock(SysUserMapper.class),
                mock(PermissionService.class),
                mock(UserAdminService.class),
                mock(OrgAssignmentService.class),
                mock(DeptOverviewService.class),
                mock(DeptNavigationService.class)
        );
    }

    private void authenticateAsSuperAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new CurrentUser(1L, 1L, true), null, List.of())
        );
    }

    private SysDept dept(Long id, String code) {
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setDeptCode(code);
        dept.setDeptName("固定科室");
        return dept;
    }
}
