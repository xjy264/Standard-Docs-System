package cn.datong.standard.security;

import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.mapper.SysUserMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = cookieValue(request.getCookies(), AuthCookies.AUTH_COOKIE);
        if (token != null) {
            try {
                Long userId = jwtTokenProvider.parseUserId(token);
                SysUser user = userMapper.selectById(userId);
                if (isActive(user)) {
                    CurrentUser currentUser = new CurrentUser(user.getId(), user.getDeptId(), Boolean.TRUE.equals(user.getIsSuperAdmin()));
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(currentUser, null, List.of());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    SecurityContextHolder.clearContext();
                }
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isActive(SysUser user) {
        return user != null
                && !Integer.valueOf(1).equals(user.getDeleted())
                && "APPROVED".equals(user.getApprovalStatus())
                && "ENABLED".equals(user.getStatus());
    }

    private String cookieValue(Cookie[] cookies, String name) {
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
