package cn.datong.standard.mapper;

import cn.datong.standard.entity.SysDocItemAttachment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface SysDocItemAttachmentMapper extends BaseMapper<SysDocItemAttachment> {
    @Update("""
            UPDATE sys_doc_item_attachment
            SET deleted = 1, deleted_at = #{deletedAt}, deleted_by = #{deletedBy}
            WHERE id = #{id} AND deleted = 0
            """)
    int softDeleteById(@Param("id") Long id, @Param("deletedBy") Long deletedBy, @Param("deletedAt") LocalDateTime deletedAt);

    @Select("SELECT * FROM sys_doc_item_attachment WHERE item_id = #{itemId}")
    List<SysDocItemAttachment> selectAllByItemIdIncludingDeleted(@Param("itemId") Long itemId);

    @Select("""
            SELECT * FROM sys_doc_item_attachment
            WHERE deleted = 1
              AND deleted_at IS NOT NULL
              AND deleted_at < #{cutoff}
            """)
    List<SysDocItemAttachment> selectExpiredDeletedAttachments(@Param("cutoff") LocalDateTime cutoff);

    @Delete("DELETE FROM sys_doc_item_attachment WHERE item_id = #{itemId}")
    int deletePhysicalByItemId(@Param("itemId") Long itemId);

    @Delete("DELETE FROM sys_doc_item_attachment WHERE id = #{id}")
    int hardDeleteById(@Param("id") Long id);
}
