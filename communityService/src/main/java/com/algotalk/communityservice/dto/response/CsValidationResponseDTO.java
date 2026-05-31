package com.algotalk.communityservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CsValidationResponseDTO(
        List<CsValidationItemDTO> results  // 검증 결과 목록
) {}