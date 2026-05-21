package com.algotalk.interviewservice.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SessionQuestionCommand {
    private Long sessionQuestionId;         // 세션 질문 ID (INSERT 후 채워짐)
    private Long sessionId;                 // 세션 ID (FK -> INTERVIEW_SESSION)
    private Long userId;                   // 사용자 ID (CREATED_BY, UPDATED_BY 용)
    private Long refId;                     // 원본 질문 ID(SCRAP_REFERENCE 전용, LLM_GENERATED과 USER_INPUT은 null)
    private String questionText;            // 질문 텍스트
    private String sourceType;              // 질문 출처 (LLM_GENERATED / USER_INPUT / SCRAP_REFERENCE)
    private int questionOrder;              // 질문 순서 (1부터 시작)
    private String difficulty;             // 난이도 (EASY / MEDIUM / HARD, LLM_GENERATED 전용)
    private String questionIntent;          // 출제 의도 (LLM_GENERATED 전용, 나머지는 null)
    private List<String> questionKeywords;  // 핵심 키워드 목록 (LLM_GENERATED 전용, 나머지는 null)
}