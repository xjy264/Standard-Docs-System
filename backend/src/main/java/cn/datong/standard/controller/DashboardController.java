package cn.datong.standard.controller;

import cn.datong.standard.common.ApiResponse;
import cn.datong.standard.dto.SectionFileTreeItem;
import cn.datong.standard.entity.SysDept;
import cn.datong.standard.entity.SysDocAttachment;
import cn.datong.standard.entity.SysDocCategory;
import cn.datong.standard.entity.SysDocItem;
import cn.datong.standard.entity.SysDocSubmission;
import cn.datong.standard.entity.SysNotification;
import cn.datong.standard.entity.SysRegisterApproval;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.mapper.SysDeptMapper;
import cn.datong.standard.mapper.SysDocAttachmentMapper;
import cn.datong.standard.mapper.SysDocCategoryMapper;
import cn.datong.standard.mapper.SysDocItemMapper;
import cn.datong.standard.mapper.SysDocSubmissionMapper;
import cn.datong.standard.mapper.SysNotificationMapper;
import cn.datong.standard.mapper.SysRegisterApprovalMapper;
import cn.datong.standard.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final SysUserMapper userMapper;
    private final SysDocItemMapper docItemMapper;
    private final SysNotificationMapper notificationMapper;
    private final SysDocSubmissionMapper submissionMapper;
    private final SysDocAttachmentMapper attachmentMapper;
    private final SysRegisterApprovalMapper approvalMapper;
    private final SysDeptMapper deptMapper;
    private final SysDocCategoryMapper categoryMapper;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Long>> stats() {
        Long userCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeleted, 0));
        Long fileCount = docItemMapper.selectCount(new LambdaQueryWrapper<SysDocItem>().eq(SysDocItem::getDeleted, 0));
        Long submissionCount = submissionMapper.selectCount(new LambdaQueryWrapper<SysDocSubmission>()
                .eq(SysDocSubmission::getDeleted, 0));
        Long attachmentCount = attachmentMapper.selectCount(new LambdaQueryWrapper<SysDocAttachment>()
                .eq(SysDocAttachment::getDeleted, 0));
        Long pendingApprovalCount = approvalMapper.selectCount(new LambdaQueryWrapper<SysRegisterApproval>()
                .eq(SysRegisterApproval::getApprovalStatus, "PENDING"));
        Long unreadCount = notificationMapper.selectCount(new LambdaQueryWrapper<SysNotification>().eq(SysNotification::getReadStatus, "UNREAD"));
        Long uploadEnabledFileCount = docItemMapper.selectCount(new LambdaQueryWrapper<SysDocItem>()
                .eq(SysDocItem::getDeleted, 0)
                .eq(SysDocItem::getAttachmentEnabled, 1));
        Long sectionCount = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeleted, 0)
                .eq(SysDept::getDeptType, "SECTION"));
        return ApiResponse.success(Map.of(
                "userCount", userCount,
                "fileCount", fileCount,
                "submissionCount", submissionCount,
                "attachmentCount", attachmentCount,
                "pendingApprovalCount", pendingApprovalCount,
                "unreadCount", unreadCount,
                "uploadEnabledFileCount", uploadEnabledFileCount,
                "sectionCount", sectionCount
        ));
    }

    @GetMapping("/section-file-tree")
    public ApiResponse<List<SectionFileTreeItem>> sectionFileTree() {
        List<SysDept> depts = deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeleted, 0)
                .eq(SysDept::getStatus, "ENABLED")
                .orderByAsc(SysDept::getSortOrder)
                .orderByAsc(SysDept::getId));
        Map<Long, Long> sectionIdByCategoryId = categoryMapper.selectList(new LambdaQueryWrapper<SysDocCategory>()
                        .eq(SysDocCategory::getDeleted, 0))
                .stream()
                .collect(Collectors.toMap(SysDocCategory::getId, SysDocCategory::getSectionDeptId, (a, b) -> a));
        Map<Long, Long> fileCountByDept = new HashMap<>();
        for (SysDocItem item : docItemMapper.selectList(new LambdaQueryWrapper<SysDocItem>().eq(SysDocItem::getDeleted, 0))) {
            Long sectionDeptId = item.getSectionDeptId() == null ? sectionIdByCategoryId.get(item.getCategoryId()) : item.getSectionDeptId();
            if (sectionDeptId != null) {
                fileCountByDept.merge(sectionDeptId, 1L, Long::sum);
            }
        }
        Map<Long, List<SysDept>> childrenByParent = depts.stream()
                .collect(Collectors.groupingBy(dept -> dept.getParentId() == null ? 0L : dept.getParentId()));
        List<SectionFileTreeItem> roots = childrenByParent.getOrDefault(0L, List.of()).stream()
                .sorted(deptComparator())
                .map(dept -> toSectionFileTreeItem(dept, childrenByParent, fileCountByDept))
                .toList();
        return ApiResponse.success(roots);
    }

    private SectionFileTreeItem toSectionFileTreeItem(SysDept dept,
                                                      Map<Long, List<SysDept>> childrenByParent,
                                                      Map<Long, Long> fileCountByDept) {
        List<SectionFileTreeItem> children = childrenByParent.getOrDefault(dept.getId(), List.of()).stream()
                .sorted(deptComparator())
                .map(child -> toSectionFileTreeItem(child, childrenByParent, fileCountByDept))
                .toList();
        long childCount = children.stream().mapToLong(SectionFileTreeItem::fileCount).sum();
        long ownCount = fileCountByDept.getOrDefault(dept.getId(), 0L);
        return new SectionFileTreeItem(
                dept.getId(),
                dept.getParentId(),
                dept.getDeptName(),
                dept.getDeptType(),
                ownCount + childCount,
                children
        );
    }

    private Comparator<SysDept> deptComparator() {
        return Comparator.comparing(SysDept::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SysDept::getId, Comparator.nullsLast(Long::compareTo));
    }
}
