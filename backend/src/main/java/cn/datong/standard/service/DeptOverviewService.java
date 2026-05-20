package cn.datong.standard.service;

import cn.datong.standard.common.BusinessException;
import cn.datong.standard.entity.SysDept;
import cn.datong.standard.entity.SysDocCategory;
import cn.datong.standard.entity.SysDocItem;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.mapper.SysDeptMapper;
import cn.datong.standard.mapper.SysDocCategoryMapper;
import cn.datong.standard.mapper.SysDocItemMapper;
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
    private final SysDocCategoryMapper categoryMapper;
    private final SysDocItemMapper itemMapper;
    private final OrgAssignmentService orgAssignmentService;

    public List<DeptOverview> overview(boolean currentUserSuperAdmin) {
        if (!currentUserSuperAdmin) {
            throw new BusinessException(403, "只有超级管理员可以查看组织概览");
        }
        List<SysDept> depts = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeleted, 0)
                .orderByAsc(SysDept::getSortOrder));
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeleted, 0));
        List<SysDocCategory> categories = categoryMapper.selectList(new LambdaQueryWrapper<SysDocCategory>().eq(SysDocCategory::getDeleted, 0));
        List<SysDocItem> items = itemMapper.selectList(new LambdaQueryWrapper<SysDocItem>().eq(SysDocItem::getDeleted, 0));
        Set<Long> adminUserIds = orgAssignmentService.adminUserIds();
        Set<Long> adminRequiredDeptIds = orgAssignmentService.assignableDepts().stream()
                .map(SysDept::getId)
                .collect(Collectors.toSet());
        Map<Long, List<SysUser>> usersByDept = users.stream()
                .filter(user -> user.getDeptId() != null)
                .collect(Collectors.groupingBy(SysUser::getDeptId));
        Map<Long, Long> sectionIdByCategoryId = categories.stream()
                .collect(Collectors.toMap(SysDocCategory::getId, SysDocCategory::getSectionDeptId, (a, b) -> a));
        Map<Long, Long> fileCountByDept = items.stream()
                .map(item -> sectionIdByCategoryId.get(item.getCategoryId()))
                .filter(sectionDeptId -> sectionDeptId != null)
                .collect(Collectors.groupingBy(sectionDeptId -> sectionDeptId, Collectors.counting()));

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
