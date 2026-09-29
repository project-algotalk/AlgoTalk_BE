package com.algotalk.userservice.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminGrade {

    SUPER("최고 관리자"),
    GENERAL("일반 관리자");

    private final String label;

    public static String getLabel(String grade) {
        if (grade == null || grade.isBlank()) {
            return null;
        }

        try {
            return AdminGrade.valueOf(grade).getLabel();
        } catch (IllegalArgumentException e) {
            return grade;
        }
    }
}