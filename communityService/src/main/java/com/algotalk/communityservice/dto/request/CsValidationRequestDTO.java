package com.algotalk.communityservice.dto.request;

import lombok.Builder;

import java.util.List;

@Builder
public record CsValidationRequestDTO(
        List<String> questions  // 검증할 질문 목록
) {}