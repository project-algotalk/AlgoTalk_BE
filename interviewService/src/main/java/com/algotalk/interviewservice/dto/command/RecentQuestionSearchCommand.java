package com.algotalk.interviewservice.dto.command;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RecentQuestionSearchCommand {
    private Long userId;               // 사용자 ID
    private List<Long> categoryIds; // 카테고리 번호 목록
    private int limit;                 // 조회 개수 (기본값: 30)
}