package com.algotalk.interviewservice.exception;

import com.algotalk.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InterviewErrorCode implements ErrorCode {

    // 세션 생성
    INVALID_CATEGORY_TYPE   ("INTERVIEW_001", "유효하지 않은 카테고리 타입입니다. (COMMON_CS 또는 JOB만 허용)", HttpStatus.BAD_REQUEST),
    CATEGORY_REQUIRED       ("INTERVIEW_002", "카테고리를 최소 1개 선택해주세요.", HttpStatus.BAD_REQUEST),  // ← 변경
    SESSION_CREATE_FAIL     ("INTERVIEW_003", "면접 세션 생성 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    QUESTION_INSERT_FAILED  ("INTERVIEW_004", "면접 질문 생성 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_CATEGORY_ID("INTERVIEW_005", "존재하지 않거나 허용되지 않는 카테고리입니다.", HttpStatus.BAD_REQUEST),
    AI_CALL_FAILED("INTERVIEW_006", "AI 질문 생성 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", HttpStatus.BAD_GATEWAY),
    INVALID_CS_QUESTION("INTERVIEW_007", "CS 기술면접과 관련없는 질문이 포함되어 있습니다.", HttpStatus.BAD_REQUEST),
    MANUAL_QUESTION_REQUIRED("INTERVIEW_008", "직접입력 질문을 최소 1개 입력해주세요.", HttpStatus.BAD_REQUEST),
    CS_CATEGORY_FETCH_FAILED("INTERVIEW_009", "카테고리 정보를 불러오는 데 실패했습니다. 잠시 후 다시 시도해주세요.", HttpStatus.BAD_GATEWAY),
    AI_EVAL_FAILED("INTERVIEW_010", "답변 평가 중 오류가 발생했습니다. 평가 결과는 나중에 확인해주세요.", HttpStatus.BAD_GATEWAY),

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