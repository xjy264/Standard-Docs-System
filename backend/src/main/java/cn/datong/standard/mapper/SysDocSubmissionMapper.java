package cn.datong.standard.mapper;

import cn.datong.standard.entity.SysDocSubmission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface SysDocSubmissionMapper extends BaseMapper<SysDocSubmission> {
    @Delete("DELETE FROM sys_doc_submission WHERE item_id = #{itemId}")
    int deletePhysicalByItemId(@Param("itemId") Long itemId);
}
