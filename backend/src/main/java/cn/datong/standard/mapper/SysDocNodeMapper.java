package cn.datong.standard.mapper;

import cn.datong.standard.dto.DocRecycleBinItem;
import cn.datong.standard.entity.SysDocNode;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface SysDocNodeMapper extends BaseMapper<SysDocNode> {
    @Update("""
            UPDATE sys_doc_node
            SET deleted = 1, deleted_at = #{deletedAt}, deleted_by = #{deletedBy}, updated_at = #{deletedAt}
            WHERE id = #{id} AND deleted = 0
            """)
    int softDeleteFileNode(@Param("id") Long id, @Param("deletedBy") Long deletedBy, @Param("deletedAt") LocalDateTime deletedAt);

    @Select("SELECT * FROM sys_doc_node WHERE id = #{id}")
    SysDocNode selectIncludingDeleted(@Param("id") Long id);

    @Update("""
            UPDATE sys_doc_node
            SET deleted = 0, parent_id = #{targetParentId}, level = #{level}, deleted_at = NULL, deleted_by = NULL, updated_at = NOW()
            WHERE id = #{id} AND deleted = 1
            """)
    int restoreFileNode(@Param("id") Long id, @Param("targetParentId") Long targetParentId, @Param("level") Integer level);

    @Select("""
            SELECT
              node.id,
              node.item_id,
              node.node_name,
              item.file_type,
              node.doc_year,
              node.deleted_at,
              COALESCE(user.real_name, user.username, user.phone) AS deleted_by_name,
              parent.node_name AS original_parent_name,
              (SELECT COUNT(*) FROM sys_doc_submission submission WHERE submission.item_id = node.item_id) AS submission_count,
              (SELECT COUNT(*) FROM sys_doc_item_attachment attachment
                 WHERE attachment.item_id = node.item_id AND attachment.deleted = 0) AS attachment_count
            FROM sys_doc_node node
            LEFT JOIN sys_doc_item item ON item.id = node.item_id
            LEFT JOIN sys_doc_node parent ON parent.id = node.parent_id
            LEFT JOIN sys_user user ON user.id = node.deleted_by
            WHERE node.section_dept_id = #{sectionDeptId}
              AND node.node_type = 'FILE'
              AND node.deleted = 1
            ORDER BY node.deleted_at DESC, node.id DESC
            """)
    List<DocRecycleBinItem> selectRecycleBinItems(@Param("sectionDeptId") Long sectionDeptId);

    @Select("""
            SELECT *
            FROM sys_doc_node
            WHERE node_type = 'FILE'
              AND deleted = 1
              AND deleted_at IS NOT NULL
              AND deleted_at < #{cutoff}
            ORDER BY deleted_at ASC, id ASC
            """)
    List<SysDocNode> selectExpiredDeletedFileNodes(@Param("cutoff") LocalDateTime cutoff);

    @Delete("DELETE FROM sys_doc_node WHERE id = #{id}")
    int hardDeleteById(@Param("id") Long id);
}
