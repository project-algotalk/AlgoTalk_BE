package com.algotalk.userservice.exception;

import com.algotalk.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    // 로그인
    LOGIN_FAIL              ("USER_001", "아이디 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED          ("USER_002", "로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.FORBIDDEN),
    ACCOUNT_DELETED         ("USER_003", "탈퇴한 계정입니다.", HttpStatus.UNAUTHORIZED),
    USER_NOT_FOUND          ("USER_004", "존재하지 않는 사용자입니다.", HttpStatus.UNAUTHORIZED),

    // 회원가입
    DUPLICATE_LOGIN_ID      ("USER_020", "이미 사용 중인 아이디입니다.", HttpStatus.CONFLICT),
    DUPLICATE_EMAIL         ("USER_021", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
    DUPLICATE_NICKNAME      ("USER_022", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),
    PASSWORD_MISMATCH       ("USER_023", "비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_DATE_FORMAT     ("USER_024", "날짜 형식이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    SIGN_UP_FAIL            ("USER_030", "회원가입 처리 중 오류가 발생했습니다.", HttpStatus.BAD_REQUEST),

    // 회원 정보 변경
    // 아이디/비밀번호 변경,
    LOGIN_ID_UPDATE_FAIL            ("USER_043", "아이디 변경이 실패했습니다.", HttpStatus.BAD_REQUEST),
    FIND_PASSWORD_SESSION_EXPIRED   ("USER_031", "비밀번호 재설정 요청이 만료되었거나 유효하지 않습니다.", HttpStatus.GONE),
    PASSWORD_RESET_FAIL             ("USER_032", "비밀번호 재설정 처리 중 오류가 발생했습니다.", HttpStatus.BAD_REQUEST),
    // 로그인 정보 수정
    CUR_PASSWORD_MISMATCH           ("USER_033", "현재 비밀번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    NOW_PASSWORD_SAME               ("USER_034", "현재 비밀번호와 동일한 비밀번호로 변경할 수 없습니다.", HttpStatus.BAD_REQUEST),
    PASSWORD_ALREADY_SET            ("USER_035", "이미 비밀번호가 설정된 계정입니다.", HttpStatus.BAD_REQUEST),
    PASSWORD_UPDATE_FAIL            ("USER_036", "비밀번호 변경이 실패했습니다.", HttpStatus.BAD_REQUEST),
    NICKNAME_UPDATE_FAIL            ("USER_037", "닉네임 변경이 실패했습니다.", HttpStatus.BAD_REQUEST),
    EMAIL_UPDATE_FAIL               ("USER_038", "이메일 변경이 실패했습니다.", HttpStatus.BAD_REQUEST),
    NAME_UPDATE_FAIL                ("USER_039", "이름 변경이 실패했습니다.", HttpStatus.BAD_REQUEST),
    ADDR_UPDATE_FAIL                ("USER_040", "주소 변경이 실패했습니다.", HttpStatus.BAD_REQUEST),
    WITHDRAW_FAIL                   ("USER_041", "회원 탈퇴 처리에 실패했습니다.", HttpStatus.BAD_REQUEST),
    WITHDRAW_PASSWORD_REQUIRED      ("USER_042", "비밀번호 가입 회원은 현재 비밀번호 입력이 필요합니다.", HttpStatus.BAD_REQUEST),
    TARGET_JOB_LIMIT_EXCEEDED       ("USER_043", "목표 직무는 최대 3개까지 등록 가능합니다.", HttpStatus.BAD_REQUEST),
    TARGET_JOB_UPDATE_FAIL          ("USER_044", "목표 직무 수정에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    EMPLOYMENT_UPDATE_FAIL          ("USER_045", "재직 이력 수정에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // 소셜
    SOCIAL_ALREADY_LINKED   ("USER_050", "이미 연동된 소셜 계정입니다.", HttpStatus.CONFLICT),
    LAST_USER_METHOD        ("USER_051", "마지막 로그인 수단은 해제할 수 없습니다.", HttpStatus.BAD_REQUEST),
    OAUTH2_PROVIDER_NOT_SUPPORTED("USER_052", "지원하지 않는 소셜 로그인입니다.", HttpStatus.BAD_REQUEST),
    OAUTH2_LOGIN_FAILED          ("USER_053", "소셜 로그인에 실패했습니다.", HttpStatus.UNAUTHORIZED),
    OAUTH2_TEMP_TOKEN_NOT_FOUND  ("USER_054", "소셜 로그인 임시 토큰이 존재하지 않습니다.", HttpStatus.UNAUTHORIZED),
    OAUTH2_TEMP_TOKEN_EXPIRED    ("USER_055", "소셜 로그인 임시 토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    SOCIAL_NOT_LINKED            ("USER_056", "연동되지 않은 소셜 계정입니다.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED_LINK_REQUEST    ("USER_057", "연결 요청 주체를 확인할 수 없습니다.", HttpStatus.UNAUTHORIZED),
    SOCIAL_ALREADY_LINKED_ME     ("USER_058", "이미 연결된 소셜 계정입니다.", HttpStatus.BAD_REQUEST),
    SOCIAL_ALREADY_LINKED_OTHER  ("USER_059", "다른 계정에 이미 연결된 소셜 계정입니다.", HttpStatus.CONFLICT),
    SOCIAL_SIGN_UP_FAIL            ("USER_060", "소셜 회원가입 처리 중 오류가 발생했습니다.", HttpStatus.BAD_REQUEST),
    OAUTH2_LINK_FAILED           ("USER_061", "소셜 계정 연결에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // 권한
    UNAUTHORIZED            ("USER_900", "로그인이 필요합니다.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN               ("USER_901", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),

    // 서버
    INTERNAL_ERROR          ("USER_999", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // 토큰
    TOKEN_EXPIRED           ("AUTH_001", "토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID           ("AUTH_002", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED), // 토큰 형식 자체가 문제가 있을 때(위변조, 구조 오류 등)
    REFRESH_TOKEN_NOT_FOUND ("AUTH_003", "토큰이 존재하지 않습니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_MISMATCH          ("AUTH_004", "토큰이 일치하지 않습니다.", HttpStatus.UNAUTHORIZED), // DB에 저장된 Refresh Token과 비교해서 일치하지 않을 때

    // 이메일
    EMAIL_CODE_EXPIRED      ("EMAIL_001", "인증번호가 만료되었습니다.", HttpStatus.GONE),
    EMAIL_VERIFIED_FAIL     ("EMAIL_005", "이메일 인증 확인 처리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    EMAIL_CODE_MISMATCH     ("EMAIL_002", "인증번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_VERIFIED      ("EMAIL_003", "이메일 인증이 완료되지 않았습니다.", HttpStatus.BAD_REQUEST),
    EMAIL_SEND_FAIL         ("EMAIL_004", "이메일 인증 처리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    ;
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
