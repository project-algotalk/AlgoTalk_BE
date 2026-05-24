package com.algotalk.userservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class EmploymentInfoResponseDTO {
    private String companyName;
    private Long categoryId;
    private String categoryName;
    private LocalDate startDate;
    private LocalDate endDate;
}