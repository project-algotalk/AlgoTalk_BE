package com.algotalk.communityservice.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

@RequiredArgsConstructor
@Getter
public enum SortType {
    ASC("ASC", "등록순"),
    DESC("DESC", "최신순");

    private final String sortType;
    private final String description;

    public static SortType from(String value) {
        if (value == null || value.isBlank()) {
            return ASC; // 기본값
        }

        try {
            return SortType.valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return ASC; // 유효하지 않은 값이 들어올 경우 기본값으로 처리
        }
    }
}
