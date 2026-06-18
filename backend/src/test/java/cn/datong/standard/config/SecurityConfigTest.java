package cn.datong.standard.config;

import cn.datong.standard.security.JwtAuthenticationFilter;
import cn.datong.standard.security.JwtTokenProvider;
import cn.datong.standard.security.CsrfProtectionFilter;
import cn.datong.standard.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig
@ContextConfiguration(classes = {
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        CsrfProtectionFilter.class,
        SecurityConfigTest.FrameTestController.class,
        SecurityConfigTest.TestConfig.class
})
class SecurityConfigTest {
    @Autowired
    private WebApplicationContext context;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser
    void frameOptionsAllowSameOriginPreviewFrames() throws Exception {
        mockMvc.perform(get("/frame-test"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
    }

    @Test
    void apiDocsAreNotPublicByDefault() throws Exception {
        mockMvc.perform(get("/doc.html"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void unsafeRequestsRequireCsrfToken() throws Exception {
        mockMvc.perform(post("/write-test"))
                .andExpect(status().isForbidden());
    }

    @RestController
    static class FrameTestController {
        @GetMapping("/frame-test")
        String frameTest() {
            return "ok";
        }

        @GetMapping("/doc.html")
        String docs() {
            return "docs";
        }

        @PostMapping("/write-test")
        String writeTest() {
            return "ok";
        }
    }

    @Configuration
    @EnableWebMvc
    static class TestConfig {
        @Bean
        JwtTokenProvider jwtTokenProvider() {
            return mock(JwtTokenProvider.class);
        }

        @Bean
        SysUserMapper sysUserMapper() {
            return mock(SysUserMapper.class);
        }
    }
}
