package cn.datong.standard.service;

import cn.datong.standard.entity.SysFile;
import cn.datong.standard.entity.SysFileCopy;
import cn.datong.standard.entity.SysFileCopyRow;
import cn.datong.standard.entity.SysFilePermission;
import cn.datong.standard.entity.SysDept;
import cn.datong.standard.enums.TargetType;
import cn.datong.standard.enums.VisibilityScope;
import cn.datong.standard.mapper.SysDeptMapper;
import cn.datong.standard.mapper.SysFileCopyMapper;
import cn.datong.standard.mapper.SysFileMapper;
import cn.datong.standard.mapper.SysFilePermissionMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileAccessServiceTest {

    @Test
    void uploaderCanAccessPrivateFile() {
        FileAccessService service = new FileAccessService();
        SysFile file = SysFile.builder()
                .id(1L)
                .uploadUserId(10L)
                .deptId(20L)
                .visibilityScope(VisibilityScope.PRIVATE)
                .build();

        boolean result = service.canAccess(false, 10L, 20L, Set.of(), file, List.of(), List.of());

        assertThat(result).isTrue();
    }

    @Test
    void assignedDepartmentPermissionAllowsAccess() {
        FileAccessService service = new FileAccessService();
        SysFile file = SysFile.builder()
                .id(1L)
                .uploadUserId(10L)
                .deptId(20L)
                .visibilityScope(VisibilityScope.ASSIGNED)
                .build();

        boolean result = service.canAccess(
                false,
                30L,
                21L,
                Set.of(),
                file,
                List.of(new SysFilePermission(1L, TargetType.DEPT, 21L, "ACCESS")),
                List.of()
        );

        assertThat(result).isTrue();
    }

    @Test
    void copiedFileDoesNotGrantAccessByItself() {
        FileAccessService service = new FileAccessService();
        SysFile file = SysFile.builder()
                .id(1L)
                .uploadUserId(10L)
                .deptId(20L)
                .visibilityScope(VisibilityScope.PRIVATE)
                .build();

        boolean result = service.canAccess(
                false,
                30L,
                21L,
                Set.of(),
                file,
                List.of(),
                List.of(new SysFileCopy(1L, 10L, 30L))
        );

        assertThat(result).isFalse();
    }

    @Test
    void privateFileHiddenFromNonOwner() {
        FileAccessService service = new FileAccessService();
        SysFile file = SysFile.builder()
                .id(1L)
                .uploadUserId(10L)
                .deptId(20L)
                .visibilityScope(VisibilityScope.PRIVATE)
                .build();

        boolean result = service.canAccess(false, 30L, 20L, Set.of(), file, List.of(), List.of());

        assertThat(result).isFalse();
    }

    @Test
    void departmentFileHiddenFromOtherDepartment() {
        FileAccessService service = new FileAccessService();
        SysFile file = SysFile.builder()
                .id(1L)
                .uploadUserId(10L)
                .deptId(20L)
                .visibilityScope(VisibilityScope.DEPT)
                .build();

        boolean result = service.canAccess(false, 30L, 21L, Set.of(), file, List.of(), List.of());

        assertThat(result).isFalse();
    }

    @Test
    void assignedFileHiddenWithoutMatchingPermission() {
        FileAccessService service = new FileAccessService();
        SysFile file = SysFile.builder()
                .id(1L)
                .uploadUserId(10L)
                .deptId(20L)
                .visibilityScope(VisibilityScope.ASSIGNED)
                .build();

        boolean result = service.canAccess(
                false,
                30L,
                21L,
                Set.of(),
                file,
                List.of(new SysFilePermission(1L, TargetType.DEPT, 22L, "ACCESS")),
                List.of()
        );

        assertThat(result).isFalse();
    }

    @Test
    void publicFileAllowsLoggedInUser() {
        FileAccessService service = new FileAccessService();
        SysFile file = SysFile.builder()
                .id(1L)
                .uploadUserId(10L)
                .deptId(20L)
                .visibilityScope(VisibilityScope.PUBLIC)
                .build();

        boolean result = service.canAccess(false, 30L, 21L, Set.of(), file, List.of(), List.of());

        assertThat(result).isTrue();
    }

    @Test
    void departmentAndChildFileAllowsChildDepartmentFromServiceEntry() {
        SysFileMapper fileMapper = mock(SysFileMapper.class);
        SysFilePermissionMapper filePermissionMapper = mock(SysFilePermissionMapper.class);
        SysFileCopyMapper fileCopyMapper = mock(SysFileCopyMapper.class);
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysFile file = SysFile.builder()
                .id(1L)
                .uploadUserId(10L)
                .deptId(20L)
                .visibilityScope(VisibilityScope.DEPT_AND_CHILD)
                .build();
        SysDept topDept = new SysDept();
        topDept.setId(20L);
        topDept.setParentId(0L);
        topDept.setDeptName("测试车间");
        SysDept childDept = new SysDept();
        childDept.setId(21L);
        childDept.setParentId(20L);
        SysDept grandchildDept = new SysDept();
        grandchildDept.setId(22L);
        grandchildDept.setParentId(21L);
        when(fileMapper.selectById(1L)).thenReturn(file);
        when(filePermissionMapper.selectList(any())).thenReturn(List.of());
        when(fileCopyMapper.selectList(any())).thenReturn(List.of());
        when(deptMapper.selectList(any())).thenReturn(List.of(topDept, childDept, grandchildDept));
        FileAccessService service = new FileAccessService(fileMapper, filePermissionMapper, fileCopyMapper, deptMapper);

        boolean childResult = service.canAccess(30L, 21L, false, 1L);
        boolean grandchildResult = service.canAccess(31L, 22L, false, 1L);

        assertThat(childResult).isTrue();
        assertThat(grandchildResult).isTrue();
    }

    @Test
    void sectionUploadedFileAllowsAllUsersIgnoringHistoricalScope() {
        SysFileMapper fileMapper = mock(SysFileMapper.class);
        SysFilePermissionMapper filePermissionMapper = mock(SysFilePermissionMapper.class);
        SysFileCopyMapper fileCopyMapper = mock(SysFileCopyMapper.class);
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysFile file = SysFile.builder()
                .id(1L)
                .uploadUserId(10L)
                .deptId(25L)
                .visibilityScope(VisibilityScope.PRIVATE)
                .build();
        when(fileMapper.selectById(1L)).thenReturn(file);
        when(filePermissionMapper.selectList(any())).thenReturn(List.of());
        when(fileCopyMapper.selectList(any())).thenReturn(List.of());
        when(deptMapper.selectList(any())).thenReturn(List.of(
                dept(24L, 0L, "机关"),
                dept(25L, 24L, "计财科"),
                dept(26L, 24L, "技术科"),
                dept(7L, 0L, "秦皇岛房建车间"),
                dept(8L, 0L, "秦皇岛公寓车间")
        ));
        FileAccessService service = new FileAccessService(fileMapper, filePermissionMapper, fileCopyMapper, deptMapper);

        assertThat(service.canAccess(32L, 25L, false, 1L)).isTrue();
        assertThat(service.canAccess(33L, 26L, false, 1L)).isTrue();
        assertThat(service.canAccess(30L, 7L, false, 1L)).isTrue();
        assertThat(service.canAccess(31L, 8L, false, 1L)).isTrue();
    }

    @Test
    void workshopUploadedFileAllowsSameWorkshopAndSectionsOnly() {
        SysFileMapper fileMapper = mock(SysFileMapper.class);
        SysFilePermissionMapper filePermissionMapper = mock(SysFilePermissionMapper.class);
        SysFileCopyMapper fileCopyMapper = mock(SysFileCopyMapper.class);
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysFile file = SysFile.builder()
                .id(1L)
                .uploadUserId(10L)
                .deptId(11L)
                .visibilityScope(VisibilityScope.PUBLIC)
                .build();
        when(fileMapper.selectById(1L)).thenReturn(file);
        when(filePermissionMapper.selectList(any())).thenReturn(List.of());
        when(fileCopyMapper.selectList(any())).thenReturn(List.of());
        when(deptMapper.selectList(any())).thenReturn(List.of(
                dept(24L, 0L, "机关"),
                dept(25L, 24L, "计财科"),
                dept(7L, 0L, "秦皇岛房建车间"),
                dept(11L, 7L, "秦皇岛房建维修班组"),
                dept(8L, 0L, "秦皇岛公寓车间")
        ));
        FileAccessService service = new FileAccessService(fileMapper, filePermissionMapper, fileCopyMapper, deptMapper);

        assertThat(service.canAccess(30L, 7L, false, 1L)).isTrue();
        assertThat(service.canAccess(31L, 11L, false, 1L)).isTrue();
        assertThat(service.canAccess(32L, 25L, false, 1L)).isTrue();
        assertThat(service.canAccess(33L, 8L, false, 1L)).isFalse();
    }

    @Test
    void copiedFileDoesNotAllowAccessOutsideOrganizationRule() {
        SysFileMapper fileMapper = mock(SysFileMapper.class);
        SysFilePermissionMapper filePermissionMapper = mock(SysFilePermissionMapper.class);
        SysFileCopyMapper fileCopyMapper = mock(SysFileCopyMapper.class);
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysFile file = SysFile.builder()
                .id(1L)
                .uploadUserId(10L)
                .deptId(7L)
                .visibilityScope(VisibilityScope.ASSIGNED)
                .build();
        when(fileMapper.selectById(1L)).thenReturn(file);
        when(filePermissionMapper.selectList(any())).thenReturn(List.of());
        when(fileCopyMapper.selectList(any())).thenReturn(List.of(copy(1L, 10L, 33L)));
        when(deptMapper.selectList(any())).thenReturn(List.of(
                dept(24L, 0L, "机关"),
                dept(25L, 24L, "计财科"),
                dept(7L, 0L, "秦皇岛房建车间"),
                dept(8L, 0L, "秦皇岛公寓车间")
        ));
        FileAccessService service = new FileAccessService(fileMapper, filePermissionMapper, fileCopyMapper, deptMapper);

        assertThat(service.canAccess(33L, 8L, false, 1L)).isFalse();
    }

    @Test
    void superAdminCanAccessAnyExistingFileFromServiceEntry() {
        SysFileMapper fileMapper = mock(SysFileMapper.class);
        SysFilePermissionMapper filePermissionMapper = mock(SysFilePermissionMapper.class);
        SysFileCopyMapper fileCopyMapper = mock(SysFileCopyMapper.class);
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysFile file = SysFile.builder()
                .id(1L)
                .uploadUserId(10L)
                .deptId(7L)
                .visibilityScope(VisibilityScope.PRIVATE)
                .build();
        when(fileMapper.selectById(1L)).thenReturn(file);
        FileAccessService service = new FileAccessService(fileMapper, filePermissionMapper, fileCopyMapper, deptMapper);

        assertThat(service.canAccess(1L, 24L, true, 1L)).isTrue();
    }

    private SysDept dept(Long id, Long parentId, String name) {
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setParentId(parentId);
        dept.setDeptName(name);
        return dept;
    }

    private SysFileCopyRow copy(Long fileId, Long senderId, Long receiverUserId) {
        SysFileCopyRow copy = new SysFileCopyRow();
        copy.setFileId(fileId);
        copy.setSenderId(senderId);
        copy.setReceiverUserId(receiverUserId);
        return copy;
    }
}
