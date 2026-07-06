package cn.datong.standard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_doc_node_workshop_scope")
public class SysDocNodeWorkshopScope {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long nodeId;
    private Long workshopDeptId;
    private LocalDateTime createdAt;
}
