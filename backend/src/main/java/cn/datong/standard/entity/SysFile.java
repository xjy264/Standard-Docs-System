package cn.datong.standard.entity;

import cn.datong.standard.enums.VisibilityScope;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_file")
public class SysFile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fileName;
    private String originalFileName;
    private String extension;
    private String mimeType;
    private Long fileSize;
    private String storageBucket;
    private String storagePath;
    private Long folderId;
    private Long deptId;
    private Long uploadUserId;
    @EnumValue
    private VisibilityScope visibilityScope;
    private String status;
    private Integer versionNo;
    private Long downloadCount;
    private Long previewCount;
    private LocalDateTime lastViewTime;
    private LocalDateTime lastEditTime;
    private Integer deleted;
    private Long deletedBy;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String remark;
}
