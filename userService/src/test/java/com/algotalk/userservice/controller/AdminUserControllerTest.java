package com.algotalk.userservice.controller;

import com.algotalk.userservice.config.SecurityConfig;
import com.algotalk.userservice.service.IAdminUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminUserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(AdminUserControllerTest.TestSecurityConfig.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IAdminUserService adminUserService;

    @Test
    @DisplayName("회원 계정 잠금 해제 성공 - 최고 관리자")
    void unlockUser_success_superAdmin() throws Exception {
        // when, then
        mockMvc.perform(
                        patch("/admin/v1/users/10/unlock")
                                .with(jwt()
                                        .jwt(jwt -> jwt.subject("1"))
                                        .authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                )
                .andExpect(status().isOk());

        verify(adminUserService).unlockUser(10L, 1L);
    }

    @Test
    @DisplayName("회원 계정 잠금 해제 실패 - 일반 관리자")
    void unlockUser_fail_admin() throws Exception {
        // when, then
        mockMvc.perform(
                        patch("/admin/v1/users/10/unlock")
                                .with(jwt()
                                        .jwt(jwt -> jwt.subject("2"))
                                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminUserService);
    }

    @Test
    @DisplayName("회원 계정 잠금 해제 실패 - 인증 정보 없음")
    void unlockUser_fail_unauthenticated() throws Exception {
        // when, then
        mockMvc.perform(patch("/admin/v1/users/10/unlock"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(adminUserService);
    }

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testSecurityFilterChain(
                HttpSecurity http
        ) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/admin/v1/users/**")
                            .hasRole("SUPER_ADMIN")
                            .anyRequest()
                            .authenticated()
                    )
                    .exceptionHandling(exception -> exception
                            .authenticationEntryPoint(
                                    new HttpStatusEntryPoint(
                                            HttpStatus.UNAUTHORIZED
                                    )
                            )
                    )
                    .build();
        }
    }
}