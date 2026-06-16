package cn.datong.standard.mapper;

import cn.datong.standard.entity.SysDocItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface SysDocItemMapper extends BaseMapper<SysDocItem> {
    @Select("SELECT * FROM sys_doc_item WHERE id = #{id}")
    SysDocItem selectIncludingDeleted(@Param("id") Long id);

    @Update("UPDATE sys_doc_item SET deleted = 0, updated_at = NOW() WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Long id);

    @Delete("DELETE FROM sys_doc_item WHERE id = #{id}")
    int hardDeleteById(@Param("id") Long id);
}
