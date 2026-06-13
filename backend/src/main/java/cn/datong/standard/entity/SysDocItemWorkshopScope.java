package cn.datong.standard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_doc_item_workshop_scope")
public class SysDocItemWorkshopScope {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long itemId;
    private Long workshopDeptId;
    private LocalDateTime createdAt;
}
