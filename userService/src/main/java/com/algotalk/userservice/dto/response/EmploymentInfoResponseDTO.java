package com.algotalk.userservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmploymentInfoResponseDTO {
    private String companyName;
    private Long categoryId;
    private String categoryName;
    private LocalDate startDate;
    private LocalDate endDate;
}