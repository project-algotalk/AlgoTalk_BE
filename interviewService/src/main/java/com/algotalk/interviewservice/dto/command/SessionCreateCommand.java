package com.algotalk.interviewservice.dto.command;

import com.algotalk.interviewservice.dto.request.CategoryItemRequestDTO;
import com.algotalk.interviewservice.dto.request.ManualQuestionItemRequestDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SessionCreateCommand {
    private Long userId; // 사용자 ID (Gateway X-User-Id 헤더에서 추출)
    private String sessionTitle; // 세션 제목
    private List<CategoryItemRequestDTO> selectedCategories; // 선택한 카테고리 목록 (최대 3개) COMMON_CS: 직무 공통 / JOB: 직무 특화
    private int questionCount; // 생성할 질문 수 (1~5개)
    private List<ManualQuestionItemRequestDTO> manualQuestions; // 직접입력 질문 목록

}