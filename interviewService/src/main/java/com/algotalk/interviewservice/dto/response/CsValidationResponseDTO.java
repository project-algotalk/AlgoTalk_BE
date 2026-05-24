package com.algotalk.interviewservice.dto.response;

import com.algotalk.interviewservice.dto.feign.CsValidationItemDTO;
import lombok.Builder;

import java.util.List;

@Builder
public record CsValidationResponseDTO(
        List<CsValidationItemDTO> results  // 검증 결과 목록
) {}