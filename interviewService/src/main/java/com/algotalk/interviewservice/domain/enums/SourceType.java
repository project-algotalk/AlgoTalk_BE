package com.algotalk.interviewservice.domain.enums;

public enum SourceType {
    LLM_GENERATED,  // LLM이 생성한 답변
    USER_INPUT,     // 사용자가 입력한 답변
    SCRAP_REFERENCE // 스크랩한 자료에서 추출한 답변
}