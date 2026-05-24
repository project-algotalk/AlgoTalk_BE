package com.algotalk.userservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class TargetJobInfoResponseDTO {
    private Long categoryId;
    private String categoryName;
    private LocalDate startDate;
    private LocalDate endDate;
}