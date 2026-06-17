package cn.datong.standard.mapper;

import cn.datong.standard.entity.SysDocSubmission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface SysDocSubmissionMapper extends BaseMapper<SysDocSubmission> {
    @Update("""
            UPDATE sys_doc_submission
            SET deleted = 1, deleted_at = #{deletedAt}, deleted_by = #{deletedBy}
            WHERE id = #{id} AND deleted = 0
            """)
    int softDeleteById(@Param("id") Long id, @Param("deletedBy") Long deletedBy, @Param("deletedAt") LocalDateTime deletedAt);

    @Delete("DELETE FROM sys_doc_submission WHERE item_id = #{itemId}")
    int deletePhysicalByItemId(@Param("itemId") Long itemId);
}
