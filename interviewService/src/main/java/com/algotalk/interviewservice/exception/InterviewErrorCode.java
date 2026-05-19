package com.algotalk.interviewservice.exception;

import com.algotalk.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InterviewErrorCode implements ErrorCode {

    // 세션 생성
    INVALID_CATEGORY_TYPE       ("INTERVIEW_001", "유효하지 않은 카테고리 타입입니다. (COMMON_CS 또는 JOB만 허용)", HttpStatus.BAD_REQUEST),
    JOB_CATEGORY_REQUIRED       ("INTERVIEW_002", "직무 특화 카테고리 1개를 선택해주세요.", HttpStatus.BAD_REQUEST),
    JOB_CATEGORY_DUPLICATED     ("INTERVIEW_003", "직무 특화 카테고리는 1개만 선택 가능합니다.", HttpStatus.BAD_REQUEST),
    SESSION_CREATE_FAIL         ("INTERVIEW_004", "면접 세션 생성 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // 권한
    UNAUTHORIZED                ("INTERVIEW_900", "로그인이 필요합니다.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN                   ("INTERVIEW_901", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),

    // 서버
    INTERNAL_ERROR              ("INTERVIEW_999", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    ;

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}