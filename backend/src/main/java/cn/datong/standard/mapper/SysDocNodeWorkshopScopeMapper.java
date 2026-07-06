package cn.datong.standard.mapper;

import cn.datong.standard.entity.SysDocNodeWorkshopScope;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface SysDocNodeWorkshopScopeMapper extends BaseMapper<SysDocNodeWorkshopScope> {
    @Delete("DELETE FROM sys_doc_node_workshop_scope WHERE node_id = #{nodeId}")
    int deletePhysicalByNodeId(@Param("nodeId") Long nodeId);
}
