package cn.datong.standard.mapper;

import cn.datong.standard.entity.SysDocUploadRequirement;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface SysDocUploadRequirementMapper extends BaseMapper<SysDocUploadRequirement> {
    @Delete("DELETE FROM sys_doc_upload_requirement WHERE item_id = #{itemId}")
    int deletePhysicalByItemId(@Param("itemId") Long itemId);
}
