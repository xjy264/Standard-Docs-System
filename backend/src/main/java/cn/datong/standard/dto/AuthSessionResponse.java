package cn.datong.standard.dto;

import java.util.Set;

public record AuthSessionResponse(AuthUser user, Set<String> permissions) {
    public static AuthSessionResponse from(AuthTokenResponse response) {
        return new AuthSessionResponse(response.user(), response.permissions());
    }
}
