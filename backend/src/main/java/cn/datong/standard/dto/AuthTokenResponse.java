package cn.datong.standard.dto;

import java.util.Set;

public record AuthTokenResponse(String token, AuthUser user, Set<String> permissions) {
}
