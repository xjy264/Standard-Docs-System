package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.entity.SysFolder;
import cn.datong.standard.mapper.SysFolderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FolderService {
    private final SysFolderMapper folderMapper;
    private final DeptNavigationService deptNavigationService;

    public List<SysFolder> list(Long userId, Long userDeptId, boolean superAdmin, Long deptId) {
        requireView(userDeptId, superAdmin, deptId);
        return folderMapper.selectList(new LambdaQueryWrapper<SysFolder>()
                .eq(SysFolder::getDeleted, 0)
                .eq(SysFolder::getDeptId, deptId)
                .orderByAsc(SysFolder::getSortOrder)
                .orderByAsc(SysFolder::getId));
    }

    public SysFolder create(Long userId, Long userDeptId, boolean superAdmin, SysFolder folder) {
        requireOperate(userDeptId, superAdmin, folder.getDeptId());
        normalizeParent(folder);
        requireParentInSameDept(folder);
        folder.setOwnerUserId(userId);
        folder.setSortOrder(folder.getSortOrder() == null ? 0 : folder.getSortOrder());
        folder.setCreatedAt(LocalDateTime.now());
        folder.setUpdatedAt(LocalDateTime.now());
        folder.setDeleted(0);
        folderMapper.insert(folder);
        return folder;
    }

    public SysFolder update(Long userDeptId, boolean superAdmin, Long id, SysFolder folder) {
        SysFolder existing = requireExisting(id);
        requireOperate(userDeptId, superAdmin, existing.getDeptId());
        folder.setId(id);
        folder.setDeptId(existing.getDeptId());
        normalizeParent(folder);
        requireParentInSameDept(folder);
        folder.setUpdatedAt(LocalDateTime.now());
        folderMapper.updateById(folder);
        return folderMapper.selectById(id);
    }

    public void delete(Long userDeptId, boolean superAdmin, Long id) {
        SysFolder existing = requireExisting(id);
        requireOperate(userDeptId, superAdmin, existing.getDeptId());
        folderMapper.deleteById(id);
    }

    private void requireView(Long userDeptId, boolean superAdmin, Long deptId) {
        if (!deptNavigationService.canViewDept(userDeptId, superAdmin, deptId)) {
            throw new BusinessException(403, "没有该组织访问权");
        }
    }

    private void requireOperate(Long userDeptId, boolean superAdmin, Long deptId) {
        if (deptId == null) {
            throw new BusinessException("请选择所属组织");
        }
        if (!superAdmin && !deptId.equals(userDeptId)) {
            throw new BusinessException(403, "只能操作自己组织的文件夹");
        }
    }

    private void normalizeParent(SysFolder folder) {
        if (folder.getParentId() == null) {
            folder.setParentId(0L);
        }
    }

    private void requireParentInSameDept(SysFolder folder) {
        if (folder.getParentId() == null || folder.getParentId() == 0) {
            return;
        }
        SysFolder parent = folderMapper.selectById(folder.getParentId());
        if (parent == null || parent.getDeleted() != null && parent.getDeleted() == 1) {
            throw new BusinessException("父级文件夹不存在");
        }
        if (!folder.getDeptId().equals(parent.getDeptId())) {
            throw new BusinessException(403, "父级文件夹不属于当前组织");
        }
    }

    private SysFolder requireExisting(Long id) {
        SysFolder existing = folderMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("文件夹不存在");
        }
        return existing;
    }
}
