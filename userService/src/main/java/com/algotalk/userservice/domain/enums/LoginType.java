package com.algotalk.userservice.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum LoginType {
    BASIC("01", "BASIC", "일반 로그인"),
    GOOGLE("02", "GOOGLE", "구글"),
    NAVER("03", "NAVER", "네이버"),
    KAKAO("04", "KAKAO", "카카오");

    private final String code;
    private final String provider;
    private final String value;
}
