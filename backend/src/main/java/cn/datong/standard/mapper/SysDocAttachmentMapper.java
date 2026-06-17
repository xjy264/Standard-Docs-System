package cn.datong.standard.mapper;

import cn.datong.standard.entity.SysDocAttachment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

import java.util.List;

public interface SysDocAttachmentMapper extends BaseMapper<SysDocAttachment> {
    @Update("""
            UPDATE sys_doc_attachment
            SET deleted = 1, deleted_at = #{deletedAt}, deleted_by = #{deletedBy}
            WHERE submission_id = #{submissionId} AND deleted = 0
            """)
    int softDeleteBySubmissionId(@Param("submissionId") Long submissionId,
                                 @Param("deletedBy") Long deletedBy,
                                 @Param("deletedAt") LocalDateTime deletedAt);

    @Select("""
            SELECT attachment.*
            FROM sys_doc_attachment attachment
            JOIN sys_doc_submission submission ON submission.id = attachment.submission_id
            WHERE submission.item_id = #{itemId}
            """)
    List<SysDocAttachment> selectAllByItemId(@Param("itemId") Long itemId);

    @Delete("""
            DELETE attachment
            FROM sys_doc_attachment attachment
            JOIN sys_doc_submission submission ON submission.id = attachment.submission_id
            WHERE submission.item_id = #{itemId}
            """)
    int deletePhysicalByItemId(@Param("itemId") Long itemId);
}
