package com.algotalk.userservice.dto.command;

import com.algotalk.userservice.dto.request.SignUpRequestDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Service 계층에서 Controller로부터 전달받은 회원정보를 담는 DTO
 *
 * DB 테이블과 1:1 매핑되는 DTO는 아니며
 * 회원가입, 로그인, 마이페이지 등 다양한 상황에서 필요한 정보를 담을 수 있도록 설계
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoCommand {
    // USERS
    private Long userId;
    private String email;       // 암호화한 이메일
    private String nickname;
    private String name;
    private String profileImgUrl;
    private String addr1;
    private String addr2;

    // USER_CREDENTIAL
    private String loginId;
    private String password;    // 암호화한 비밀번호

    // USER_CREDENTIAL
    String role; // List<String> roles -> String role (단일 역할)로 변경, "ROLE_USER", "ROLE_ADMIN" 등으로 저장

    // USER_TARGET_JOB, USER_EMPLOYMENT
    private Long categoryId;
    private String categoryName;
    private String companyName;
    private LocalDate startDate;
    private LocalDate endDate;

    // 가상 컬럼
    /**
     * DB 테이블에 존재하지 않는 가상의 컬럼
     * 회원 가입시 중복 체크 용으로 사용(Y/N)
     */
    private String existsYn;

    /**
     * DB 테이블에 존재하지 않는 가상의 컬럼
     * 회원 가입시 인증번호 발송 후 검증 용으로 사용
     */
    private int authNumber;

    public static UserInfoCommand from(SignUpRequestDTO pDTO) {
        return UserInfoCommand.builder()
                .email(pDTO.email())
                .nickname(pDTO.resolvedNickname())
                .name(pDTO.name())
                .addr1(pDTO.addr1())
                .addr2(pDTO.addr2())
                .loginId(pDTO.loginId())
                .password(pDTO.password())
                .build();
    }
}
