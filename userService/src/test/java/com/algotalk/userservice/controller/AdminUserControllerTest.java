package com.algotalk.userservice.controller;

import com.algotalk.userservice.config.SecurityConfig;
import com.algotalk.userservice.dto.request.AdminUserListRequestDTO;
import com.algotalk.userservice.dto.response.AdminUserPageResponseDTO;
import com.algotalk.userservice.dto.response.AdminUserResponseDTO;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    @DisplayName("일반 회원 목록 조회 성공 - 최고 관리자")
    void getUsers_success_superAdmin() throws Exception {
        // given
        AdminUserPageResponseDTO response = new AdminUserPageResponseDTO(
                List.of(new AdminUserResponseDTO(
                        "user01",
                        "테스터",
                        "테스트",
                        LocalDateTime.of(2026, 9, 26, 10, 0),
                        true
                )),
                1,
                10,
                1L,
                1
        );

        given(adminUserService.getUsers(any(AdminUserListRequestDTO.class)))
                .willReturn(response);

        // when, then
        mockMvc.perform(
                        get("/admin/v1/users")
                                .param("page", "1")
                                .param("size", "10")
                                .param("keyword", "user01")
                                .with(jwt()
                                        .jwt(jwt -> jwt.subject("1"))
                                        .authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].loginId").value("user01"))
                .andExpect(jsonPath("$.data.content[0].locked").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(1));

        verify(adminUserService).getUsers(
                new AdminUserListRequestDTO(1, 10, "user01")
        );
    }

    @Test
    @DisplayName("일반 회원 목록 조회 성공 - 일반 관리자")
    void getUsers_success_admin() throws Exception {
        // given
        given(adminUserService.getUsers(any(AdminUserListRequestDTO.class)))
                .willReturn(new AdminUserPageResponseDTO(
                        List.of(),
                        1,
                        10,
                        0L,
                        0
                ));

        // when, then
        mockMvc.perform(
                        get("/admin/v1/users")
                                .with(jwt()
                                        .jwt(jwt -> jwt.subject("2"))
                                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                )
                .andExpect(status().isOk());

        verify(adminUserService).getUsers(
                new AdminUserListRequestDTO(null, null, null)
        );
    }

    @Test
    @DisplayName("일반 회원 목록 조회 실패 - 일반 회원")
    void getUsers_fail_user() throws Exception {
        // when, then
        mockMvc.perform(
                        get("/admin/v1/users")
                                .with(jwt()
                                        .jwt(jwt -> jwt.subject("10"))
                                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminUserService);
    }

    @Test
    @DisplayName("회원 계정 잠금 해제 성공 - 최고 관리자")
    void unlockUser_success_superAdmin() throws Exception {
        // when, then
        mockMvc.perform(
                        patch("/admin/v1/users/unlock")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "loginId": "user01"
                                        }
                                        """)
                                .with(jwt()
                                        .jwt(jwt -> jwt.subject("1"))
                                        .authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                )
                .andExpect(status().isOk());

        verify(adminUserService).unlockUser("user01", 1L);
    }

    @Test
    @DisplayName("회원 계정 잠금 해제 성공 - 일반 관리자")
    void unlockUser_success_admin() throws Exception {
        // when, then
        mockMvc.perform(
                        patch("/admin/v1/users/unlock")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "loginId": "user01"
                                        }
                                        """)
                                .with(jwt()
                                        .jwt(jwt -> jwt.subject("2"))
                                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                )
                .andExpect(status().isOk());

        verify(adminUserService).unlockUser("user01", 2L);
    }

    @Test
    @DisplayName("회원 계정 잠금 해제 실패 - 일반 회원")
    void unlockUser_fail_user() throws Exception {
        // when, then
        mockMvc.perform(
                        patch("/admin/v1/users/unlock")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "loginId": "user01"
                                        }
                                        """)
                                .with(jwt()
                                        .jwt(jwt -> jwt.subject("10"))
                                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminUserService);
    }

    @Test
    @DisplayName("회원 계정 잠금 해제 실패 - 인증 정보 없음")
    void unlockUser_fail_unauthenticated() throws Exception {
        // when, then
        mockMvc.perform(
                        patch("/admin/v1/users/unlock")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "loginId": "user01"
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(adminUserService);
    }

    @Test
    @DisplayName("회원 계정 잠금 해제 실패 - 로그인 아이디 누락")
    void unlockUser_fail_blankLoginId() throws Exception {
        // when, then
        mockMvc.perform(
                        patch("/admin/v1/users/unlock")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "loginId": ""
                                        }
                                        """)
                                .with(jwt()
                                        .jwt(jwt -> jwt.subject("1"))
                                        .authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                )
                .andExpect(status().isBadRequest());

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
                            .requestMatchers(
                                    "/admin/v1/users",
                                    "/admin/v1/users/**"
                            )
                            .hasAnyRole("ADMIN", "SUPER_ADMIN")
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
