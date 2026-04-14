package com.algotalk.userservice.repository;

import com.algotalk.userservice.dto.command.UserInfoCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class UserLoginMapperTest {

    @Autowired
    IUserLoginMapper userLoginMapper;

    @Autowired
    IUserRegMapper userRegMapper;

    @Test
    @Transactional
    @DisplayName("존재하는 loginId 조회 - 인증 정보 반환")
    void getUserAuthInfo_exists() throws Exception {
        // given
        String loginId = "login01";
        String name = "테스트";
        String nickname = "테스트1";
        String email = loginId + "@algotalk.com";
        String password = "$2a$10$hashedpassword";
        String role = "ROLE_USER";

        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname(nickname)
                .name(name)
                .email(email)
                .loginId(loginId)
                .password(password)
                .role(role)
                .build();
        userRegMapper.insertUser(cmd);
        userRegMapper.insertUserCredential(cmd);
        userRegMapper.insertUserRoles(cmd);

        UserInfoCommand pCommand = UserInfoCommand.builder()
                .loginId(loginId)
                .build();

        // when
        UserInfoCommand result = userLoginMapper.getUserAuthInfo(pCommand);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isNotNull();
        assertThat(result.getLoginId()).isNotNull();
        assertThat(result.getPassword()).isNotNull();
        assertThat(result.getRole()).isEqualTo("ROLE_USER");
        assertThat(result.getDeletedYn()).isEqualTo("N");
    }

    @Test
    @DisplayName("존재하지 않는 loginId 조회 - null 반환")
    void getUserAuthInfo_notExists() throws Exception {
        // given
        UserInfoCommand pCommand = UserInfoCommand.builder()
                .loginId("not_exist_id_xyz")
                .build();

        // when
        UserInfoCommand result = userLoginMapper.getUserAuthInfo(pCommand);

        // then
        assertThat(result).isNull();
    }

    @Test
    @Transactional
    @DisplayName("userId로 조회 - 인증 정보 반환")
    void getUserAuthInfo_userIdExists() throws Exception {
        // given
        String loginId = "login03";
        String name = "테스트";
        String nickname = "테스트3";
        String email = loginId + "@algotalk.com";
        String password = "$2a$10$hashedpassword";
        String role = "ROLE_USER";

        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname(nickname)
                .name(name)
                .email(email)
                .loginId(loginId)
                .password(password)
                .role(role)
                .build();
        userRegMapper.insertUser(cmd);
        userRegMapper.insertUserCredential(cmd);
        userRegMapper.insertUserRoles(cmd);

        Long userId = cmd.getUserId();

        UserInfoCommand pCommand = UserInfoCommand.builder()
                .userId(userId)
                .build();

        // when
        UserInfoCommand result = userLoginMapper.getUserAuthInfo(pCommand);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isNotNull();
        assertThat(result.getLoginId()).isNotNull();
        assertThat(result.getPassword()).isNotNull();
        assertThat(result.getRole()).isEqualTo("ROLE_USER");
        assertThat(result.getDeletedYn()).isEqualTo("N");
    }
}