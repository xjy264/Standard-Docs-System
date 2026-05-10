package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.entity.SysDept;
import cn.datong.standard.entity.SysFile;
import cn.datong.standard.entity.SysFileCopy;
import cn.datong.standard.entity.SysFilePermission;
import cn.datong.standard.enums.TargetType;
import cn.datong.standard.enums.VisibilityScope;
import cn.datong.standard.mapper.SysDeptMapper;
import cn.datong.standard.mapper.SysFileCopyMapper;
import cn.datong.standard.mapper.SysFileMapper;
import cn.datong.standard.mapper.SysFilePermissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class FileAccessService {
    private SysFileMapper fileMapper;
    private SysFilePermissionMapper filePermissionMapper;
    private SysFileCopyMapper fileCopyMapper;
    private SysDeptMapper deptMapper;

    public FileAccessService() {
    }

    @Autowired
    public FileAccessService(SysFileMapper fileMapper, SysFilePermissionMapper filePermissionMapper,
                             SysFileCopyMapper fileCopyMapper, SysDeptMapper deptMapper) {
        this.fileMapper = fileMapper;
        this.filePermissionMapper = filePermissionMapper;
        this.fileCopyMapper = fileCopyMapper;
        this.deptMapper = deptMapper;
    }

    public boolean canAccess(boolean superAdmin, Long userId, Long userDeptId, Set<Long> childDeptIds,
                             SysFile file, List<SysFilePermission> permissions, List<SysFileCopy> copies) {
        if (superAdmin) {
            return true;
        }
        if (file == null) {
            return false;
        }
        if (userId != null && userId.equals(file.getUploadUserId())) {
            return true;
        }
        VisibilityScope scope = file.getVisibilityScope();
        if (scope == VisibilityScope.PUBLIC) {
            return true;
        }
        if (scope == VisibilityScope.PRIVATE) {
            return false;
        }
        if (scope == VisibilityScope.DEPT && userDeptId != null && userDeptId.equals(file.getDeptId())) {
            return true;
        }
        if (scope == VisibilityScope.DEPT_AND_CHILD && userDeptId != null
                && (userDeptId.equals(file.getDeptId()) || childDeptIds.contains(userDeptId))) {
            return true;
        }
        if (scope == VisibilityScope.ASSIGNED && permissions.stream().anyMatch(permission ->
                "ACCESS".equals(permission.accessType())
                        && ((permission.targetType() == TargetType.USER && permission.targetId().equals(userId))
                        || (permission.targetType() == TargetType.DEPT && permission.targetId().equals(userDeptId))))) {
            return true;
        }
        return false;
    }

    public boolean canAccess(Long userId, Long userDeptId, boolean superAdmin, Long fileId) {
        SysFile file = fileMapper.selectById(fileId);
        if (file == null) {
            return false;
        }
        return canAccessByOrganization(userId, userDeptId, file);
    }

    public void requireAccess(Long userId, Long userDeptId, boolean superAdmin, Long fileId) {
        if (!canAccess(userId, userDeptId, superAdmin, fileId)) {
            throw new BusinessException(403, "没有该文件访问权");
        }
    }

    private boolean canAccessByOrganization(Long userId, Long userDeptId, SysFile file) {
        if (file == null) {
            return false;
        }
        if (userId != null && userId.equals(file.getUploadUserId())) {
            return true;
        }
        if (userDeptId == null || file.getDeptId() == null || deptMapper == null) {
            return false;
        }
        List<SysDept> depts = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeleted, 0));
        if (depts == null || depts.isEmpty()) {
            return false;
        }
        Long agencyDeptId = agencyDeptId(depts);
        Long fileTopDeptId = OrgScopeSupport.topLevelDeptId(file.getDeptId(), depts);
        Long userTopDeptId = OrgScopeSupport.topLevelDeptId(userDeptId, depts);
        if (agencyDeptId != null && agencyDeptId.equals(fileTopDeptId)) {
            return true;
        }
        if (agencyDeptId != null && agencyDeptId.equals(userTopDeptId)) {
            return true;
        }
        return fileTopDeptId != null && fileTopDeptId.equals(userTopDeptId);
    }

    private Long agencyDeptId(List<SysDept> depts) {
        for (SysDept dept : OrgScopeSupport.topLevelDepts(depts)) {
            if ("机关".equals(dept.getDeptName())) {
                return dept.getId();
            }
        }
        return null;
    }
}
