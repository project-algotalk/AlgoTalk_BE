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

    // 토큰
    TOKEN_EXPIRED           ("AUTH_001", "액세스 토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID           ("AUTH_002", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_FOUND ("AUTH_003", "리프레시 토큰이 존재하지 않습니다.", HttpStatus.UNAUTHORIZED),

    // 회원가입
    DUPLICATE_LOGIN_ID      ("USER_020", "이미 사용 중인 로그인 아이디입니다.", HttpStatus.CONFLICT),
    DUPLICATE_EMAIL         ("USER_021", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
    DUPLICATE_NICKNAME      ("USER_022", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),
    PASSWORD_MISMATCH       ("USER_023", "비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),
    SIGN_UP_FAIL            ("USER_030", "회원가입에 처리 중 오류가 발생했습니다.", HttpStatus.BAD_REQUEST),

    // 이메일
    EMAIL_CODE_EXPIRED      ("EMAIL_001", "인증번호가 만료되었습니다.", HttpStatus.GONE),
    EMAIL_CODE_MISMATCH     ("EMAIL_002", "인증번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_VERIFIED      ("EMAIL_003", "이메일 인증이 완료되지 않았습니다.", HttpStatus.BAD_REQUEST),
    EMAIL_SEND_FAIL         ("EMAIL_004", "이메일 인증 처리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    EMAIL_VERIFIED_FAIL     ("EMAIL_005", "이메일 인증 확인 처리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // 소셜
    SOCIAL_ALREADY_LINKED   ("USER_040", "이미 연동된 소셜 계정입니다.", HttpStatus.CONFLICT),
    LAST_USER_METHOD        ("USER_041", "마지막 로그인 수단은 해제할 수 없습니다.", HttpStatus.BAD_REQUEST),

    // 권한
    UNAUTHORIZED            ("USER_900", "로그인이 필요합니다.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN               ("USER_901", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),

    // 서버
    INTERNAL_ERROR          ("USER_999", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
