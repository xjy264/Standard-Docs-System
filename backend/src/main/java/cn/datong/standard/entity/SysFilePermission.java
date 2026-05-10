package cn.datong.standard.entity;

import cn.datong.standard.enums.TargetType;

public record SysFilePermission(Long fileId, TargetType targetType, Long targetId, String accessType) {
}
