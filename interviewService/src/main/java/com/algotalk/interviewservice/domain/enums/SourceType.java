package com.algotalk.interviewservice.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SourceType {
    LLM_GENERATED("LLM_GENERATED", "AI가 생성한 질문"),
    USER_INPUT("USER_INPUT", "사용자가 직접 입력한 질문"),
    SCRAP_REFERENCE("SCRAP_REFERENCE", "커뮤니티 게시글에서 스크랩한 질문");

    private final String type;
    private final String description;
}