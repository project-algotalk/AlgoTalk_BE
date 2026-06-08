package com.algotalk.userservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TargetJobInfoResponseDTO {
    private Long categoryId;
    private String categoryName;
    private LocalDate startDate;
    private LocalDate endDate;
}