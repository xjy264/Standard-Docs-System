package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.entity.SysFolder;
import cn.datong.standard.mapper.SysFolderMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FolderServiceTest {

    @Test
    void listRequiresVisibleOrganization() {
        SysFolderMapper folderMapper = mock(SysFolderMapper.class);
        DeptNavigationService deptNavigationService = mock(DeptNavigationService.class);
        when(deptNavigationService.canViewDept(7L, false, 8L)).thenReturn(false);
        FolderService service = new FolderService(folderMapper, deptNavigationService);

        assertThatThrownBy(() -> service.list(30L, 7L, false, 8L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("没有该组织访问权");
    }

    @Test
    void userCannotCreateFolderForOtherOrganization() {
        SysFolderMapper folderMapper = mock(SysFolderMapper.class);
        DeptNavigationService deptNavigationService = mock(DeptNavigationService.class);
        FolderService service = new FolderService(folderMapper, deptNavigationService);
        SysFolder folder = folder("其他组织资料", 7L, 0L);

        assertThatThrownBy(() -> service.create(30L, 25L, false, folder))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只能操作自己组织的文件夹");
    }

    @Test
    void superAdminCanCreateFolderForAnyOrganization() {
        SysFolderMapper folderMapper = mock(SysFolderMapper.class);
        DeptNavigationService deptNavigationService = mock(DeptNavigationService.class);
        FolderService service = new FolderService(folderMapper, deptNavigationService);
        SysFolder folder = folder("车间资料", 7L, 0L);

        SysFolder result = service.create(1L, 24L, true, folder);

        assertThat(result.getOwnerUserId()).isEqualTo(1L);
        assertThat(result.getDeptId()).isEqualTo(7L);
        assertThat(result.getParentId()).isEqualTo(0L);
        assertThat(result.getDeleted()).isZero();
        verify(folderMapper).insert(folder);
    }

    @Test
    void superAdminCanRenameFolderInAnyOrganization() {
        SysFolderMapper folderMapper = mock(SysFolderMapper.class);
        DeptNavigationService deptNavigationService = mock(DeptNavigationService.class);
        SysFolder existing = folder("旧名称", 7L, 0L);
        existing.setId(10L);
        SysFolder renamed = folder("新名称", 7L, 0L);
        renamed.setId(10L);
        when(folderMapper.selectById(10L)).thenReturn(existing, renamed);
        FolderService service = new FolderService(folderMapper, deptNavigationService);

        SysFolder result = service.update(null, true, 10L, folder("新名称", 99L, 0L));

        assertThat(result.getFolderName()).isEqualTo("新名称");
        verify(folderMapper).updateById(any(SysFolder.class));
    }

    @Test
    void childFolderParentMustBelongToSameOrganization() {
        SysFolderMapper folderMapper = mock(SysFolderMapper.class);
        DeptNavigationService deptNavigationService = mock(DeptNavigationService.class);
        SysFolder parent = folder("机关资料", 25L, 0L);
        parent.setId(100L);
        when(folderMapper.selectById(100L)).thenReturn(parent);
        FolderService service = new FolderService(folderMapper, deptNavigationService);

        assertThatThrownBy(() -> service.create(1L, 24L, true, folder("车间子目录", 7L, 100L)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("父级文件夹不属于当前组织");
    }

    @Test
    void listReturnsVisibleOrganizationFolders() {
        SysFolderMapper folderMapper = mock(SysFolderMapper.class);
        DeptNavigationService deptNavigationService = mock(DeptNavigationService.class);
        SysFolder folder = folder("制度文件", 25L, 0L);
        when(deptNavigationService.canViewDept(25L, false, 25L)).thenReturn(true);
        when(folderMapper.selectList(any())).thenReturn(List.of(folder));
        FolderService service = new FolderService(folderMapper, deptNavigationService);

        List<SysFolder> result = service.list(30L, 25L, false, 25L);

        assertThat(result).containsExactly(folder);
    }

    private SysFolder folder(String name, Long deptId, Long parentId) {
        SysFolder folder = new SysFolder();
        folder.setFolderName(name);
        folder.setDeptId(deptId);
        folder.setParentId(parentId);
        folder.setSortOrder(0);
        return folder;
    }
}
