package cn.datong.standard.mapper;

import cn.datong.standard.entity.SysDocItemWorkshopScope;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysDocItemWorkshopScopeMapper extends BaseMapper<SysDocItemWorkshopScope> {
    @Delete("DELETE FROM sys_doc_item_workshop_scope WHERE item_id = #{itemId}")
    int deletePhysicalByItemId(@Param("itemId") Long itemId);
}
