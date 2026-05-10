package cn.datong.standard.dto;

import cn.datong.standard.entity.SysUser;

import java.util.Set;

public record AuthTokenResponse(String token, SysUser user, Set<String> permissions) {
}
