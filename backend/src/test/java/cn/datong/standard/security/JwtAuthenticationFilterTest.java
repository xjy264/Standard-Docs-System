package cn.datong.standard.security;

import cn.datong.standard.dto.CurrentUser;
import cn.datong.standard.entity.SysUser;
import cn.datong.standard.mapper.SysUserMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesActiveUserFromDatabaseInsteadOfTrustingTokenClaims() throws ServletException, IOException {
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUser user = new SysUser();
        user.setId(7L);
        user.setDeptId(23L);
        user.setIsSuperAdmin(false);
        user.setStatus("ENABLED");
        user.setApprovalStatus("APPROVED");
        user.setDeleted(0);
        when(jwtTokenProvider.parseUserId("token")).thenReturn(7L);
        when(userMapper.selectById(7L)).thenReturn(user);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, userMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie(AuthCookies.AUTH_COOKIE, "token"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        CurrentUser currentUser = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(currentUser.userId()).isEqualTo(7L);
        assertThat(currentUser.deptId()).isEqualTo(23L);
        assertThat(currentUser.superAdmin()).isFalse();
    }

    @Test
    void rejectsDisabledUsersEvenWhenTokenIsValid() throws ServletException, IOException {
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUser user = new SysUser();
        user.setId(7L);
        user.setStatus("DISABLED");
        user.setApprovalStatus("APPROVED");
        user.setDeleted(0);
        when(jwtTokenProvider.parseUserId("token")).thenReturn(7L);
        when(userMapper.selectById(7L)).thenReturn(user);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, userMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie(AuthCookies.AUTH_COOKIE, "token"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
