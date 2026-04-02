package com.algotalk.userservice.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum LoginType {
    BASIC("01", "일반 로그인"),
    GOOGLE("02", "구글"),
    NAVER("03", "네이버"),
    KAKAO("04", "카카오");

    private final String code;
    private final String value;
}
