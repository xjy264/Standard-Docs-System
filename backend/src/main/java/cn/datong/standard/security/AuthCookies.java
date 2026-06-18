package cn.datong.standard.security;

public final class AuthCookies {
    public static final String AUTH_COOKIE = "SDS_AUTH";
    public static final String CSRF_COOKIE = "XSRF-TOKEN";
    public static final String CSRF_HEADER = "X-XSRF-TOKEN";

    private AuthCookies() {
    }
}
