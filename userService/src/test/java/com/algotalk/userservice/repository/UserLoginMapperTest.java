package com.algotalk.userservice.repository;

import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.LoginIdCheckRequestDTO;
import com.algotalk.userservice.dto.response.ExistsResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.time.LocalDate.of;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@SpringBootTest
@ActiveProfiles("local")
class UserLoginMapperTest {

    @Autowired
    IUserLoginMapper userLoginMapper;

    @Test
    @DisplayName("존재하는 loginId 조회 - 인증 정보 반환")
    void getUserAuthInfo_exists() throws Exception {
        // given: DB에 실제 존재하는 loginId
        UserInfoCommand pCommand = UserInfoCommand.builder()
                .loginId("test")
                .build();

        // when
        UserInfoCommand result = userLoginMapper.getUserAuthInfo(pCommand);

        log.info("userId: {}", result.getUserId());
        log.info("nickname: {}", result.getNickname());
        log.info("role: {}", result.getRole());
        log.info("deletedYn: {}", result.getDeletedYn());

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isNotNull();
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
        log.info("조회 결과: {}", result);

        // then
        assertThat(result).isNull();
    }
}