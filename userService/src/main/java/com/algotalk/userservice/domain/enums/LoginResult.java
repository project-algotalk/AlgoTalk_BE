package com.algotalk.userservice.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum LoginResult {
    SUCCESS("00", "로그인 성공"),
    USER_NOT_FOUND("01", "존재하지 않는 사용자"),
    INVALID_ID("02", "아이디 및 비밀번호 불일치"),
    INVALID_PASSWORD("03", "아이디 및 비밀번호 불일치"),
    ACCOUNT_LOCKED("04", "계정 잠김"),
    UNKNOWN_ERROR("99", "알 수 없는 오류");

    private final String code;
    private final String value;
}
