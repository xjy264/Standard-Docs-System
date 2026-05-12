package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.entity.SysDept;
import cn.datong.standard.entity.SysFile;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.mapper.SysDeptMapper;
import cn.datong.standard.mapper.SysFileMapper;
import cn.datong.standard.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeptOverviewService {
    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;
    private final SysFileMapper fileMapper;
    private final OrgAssignmentService orgAssignmentService;

    public List<DeptOverview> overview(boolean currentUserSuperAdmin) {
        if (!currentUserSuperAdmin) {
            throw new BusinessException(403, "只有超级管理员可以查看组织概览");
        }
        List<SysDept> depts = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeleted, 0)
                .orderByAsc(SysDept::getSortOrder));
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeleted, 0));
        List<SysFile> files = fileMapper.selectList(new LambdaQueryWrapper<SysFile>().eq(SysFile::getDeleted, 0));
        Set<Long> adminUserIds = orgAssignmentService.adminUserIds();
        Set<Long> adminRequiredDeptIds = orgAssignmentService.assignableDepts().stream()
                .map(SysDept::getId)
                .collect(Collectors.toSet());
        Map<Long, List<SysUser>> usersByDept = users.stream()
                .filter(user -> user.getDeptId() != null)
                .collect(Collectors.groupingBy(SysUser::getDeptId));
        Map<Long, Long> fileCountByDept = files.stream()
                .filter(file -> file.getDeptId() != null)
                .collect(Collectors.groupingBy(SysFile::getDeptId, Collectors.counting()));

        return depts.stream()
                .sorted(Comparator.comparing(SysDept::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(dept -> toOverview(dept, usersByDept.getOrDefault(dept.getId(), List.of()),
                        fileCountByDept.getOrDefault(dept.getId(), 0L), adminUserIds, adminRequiredDeptIds))
                .toList();
    }

    private DeptOverview toOverview(SysDept dept, List<SysUser> users, long fileCount,
                                    Set<Long> adminUserIds, Set<Long> adminRequiredDeptIds) {
        List<String> adminNames = users.stream()
                .filter(user -> adminUserIds.contains(user.getId()))
                .map(this::displayName)
                .sorted()
                .toList();
        boolean adminRequired = adminRequiredDeptIds.contains(dept.getId());
        return new DeptOverview(
                dept.getId(),
                dept.getParentId(),
                dept.getDeptName(),
                dept.getDeptCode(),
                dept.getSortOrder(),
                dept.getStatus(),
                users.size(),
                fileCount,
                adminRequired ? adminNames : List.of(),
                adminRequired && adminNames.isEmpty(),
                adminRequired
        );
    }

    private String displayName(SysUser user) {
        if (user.getRealName() != null && !user.getRealName().isBlank()) {
            return user.getRealName();
        }
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            return user.getPhone();
        }
        return user.getUsername();
    }
}
