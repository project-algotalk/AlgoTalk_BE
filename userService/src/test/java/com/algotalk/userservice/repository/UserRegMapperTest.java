package com.algotalk.userservice.repository;

import com.algotalk.userservice.dto.command.UserInfoCommand;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.time.LocalDate.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@SpringBootTest
@ActiveProfiles("local")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserRegMapperTest {

    @Autowired
    IUserRegMapper userRegMapper;

    @Test
    @DisplayName("DB 연결 확인 - USERS 테이블 조회")
    void dbConnectionTest() throws Exception {
        // USERS 테이블이 존재하고 연결되면 예외 없이 통과
        UserInfoCommand cmd = UserInfoCommand.builder()
                .loginId("connection_test")
                .build();
        UserInfoCommand result = userRegMapper.getLoginIdExists(cmd);
        log.info("DB 연결 확인 결과: {}", result.getExistsYn());
        assertThat(result).isNotNull();
        assertThat(result.getExistsYn()).isIn("Y", "N");
    }

    @Test
    @Transactional
    @DisplayName("USERS INSERT - userId 자동 채번 확인")
    void insertUserTest() throws Exception {
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("테스트유저")
                .name("홍길동")
                .email("test@algotalk.com")
                .addr1("서울시 강서구")
                .addr2("8316")
                .build();

        int result = userRegMapper.insertUser(cmd);
        log.info("USERS INSERT 결과: {} / 채번된 userId: {}", result, cmd.getUserId());

        assertThat(result).isEqualTo(1);
        assertThat(cmd.getUserId()).isNotNull();
    }

    @Test
    @Transactional
    @DisplayName("회원가입 일부 플로우 - USERS -> CREDENTIAL -> ROLES")
    void insertSignUpFlow() throws Exception {
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("플로우테스트")
                .name("테스트")
                .email("test@algotalk.com")
                .loginId("test")
                .password("$2a$10$hashedpassword")
                .role("USER")
                .build();

        // USERS INSERT (userId 채번)
        userRegMapper.insertUser(cmd);
        log.info("USERS INSERT 완료 / userId: {}", cmd.getUserId());
        assertThat(cmd.getUserId()).isNotNull();

        // USER_CREDENTIAL INSERT
        int credResult = userRegMapper.insertUserCredential(cmd);
        log.info("USER_CREDENTIAL INSERT: {}", credResult);
        assertThat(credResult).isEqualTo(1);

        // USER_ROLES INSERT
        int roleResult = userRegMapper.insertUserRoles(cmd);
        log.info("USER_ROLES INSERT: {}", roleResult);
        assertThat(roleResult).isEqualTo(1);
    }

    @Test
    @Transactional
    @DisplayName("회원가입 전체 플로우 - USERS → CREDENTIAL → ROLES → TARGET_JOB → EMPLOYMENT")
    void insertFullSignUpFlow() throws Exception {

        // 1. 기본 정보
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("플로우테스트")
                .name("테스트")
                .email("test@algotalk.com")
                .loginId("test")
                .password("$2a$10$hashedpassword")
                .role("USER")
                .build();

        // USERS INSERT (userId 채번)
        userRegMapper.insertUser(cmd);
        log.info("USERS INSERT 완료 / userId: {}", cmd.getUserId());
        assertThat(cmd.getUserId()).isNotNull();

        // USER_CREDENTIAL INSERT
        int credResult = userRegMapper.insertUserCredential(cmd);
        log.info("USER_CREDENTIAL INSERT: {}", credResult);
        assertThat(credResult).isEqualTo(1);

        // USER_ROLES INSERT
        int roleResult = userRegMapper.insertUserRoles(cmd);
        log.info("USER_ROLES INSERT: {}", roleResult);
        assertThat(roleResult).isEqualTo(1);

        // 2. 목표 직무 (여러 개 가능)
        // 실제 CS_CATEGORY 테이블에 존재하는 categoryId를 넣어야 함
        // 테스트용으로 categoryId = 1L 사용 (없으면 FK 오류)
        List<Long> targetCategoryIds = List.of(1L, 2L);  // 목표 직무 2개

        for (Long categoryId : targetCategoryIds) {
            cmd.setCategoryId(categoryId);
            cmd.setStartDate(of(2026, 1, 1));
            cmd.setEndDate(null);  // null = 현재 준비 중
            int jobResult = userRegMapper.insertUserTargetJob(cmd);
            log.info("USER_TARGET_JOB INSERT: categoryId={} / result={}", categoryId, jobResult);
            assertThat(jobResult).isEqualTo(1);
        }

        // 3. 재직 이력 (여러 개 가능)
        // 재직이력도 categoryId FK → CS_CATEGORY 존재해야 함
        List<UserInfoCommand> employments = List.of(
                UserInfoCommand.builder()
                        .categoryId(1L)
                        .companyName("알고톡주식회사")
                        .startDate(of(2026, 3, 1))
                        .endDate(of(9999, 12, 31)) // 9999-12-31 = 현재 재직 중
                        .build(),
                UserInfoCommand.builder()
                        .categoryId(2L)
                        .companyName("이전회사")
                        .startDate(of(2021, 1, 1))
                        .endDate(of(2026, 2, 28))
                        .build()
        );

        for (UserInfoCommand emp : employments) {
            emp.setUserId(cmd.getUserId());  // 채번된 userId 세팅
            int empResult = userRegMapper.insertUserEmployment(emp);
            log.info("USER_EMPLOYMENT INSERT: company={} / result={}", emp.getCompanyName(), empResult);
            assertThat(empResult).isEqualTo(1);
        }
    }

}