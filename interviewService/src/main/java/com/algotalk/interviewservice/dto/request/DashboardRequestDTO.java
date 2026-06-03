package com.algotalk.interviewservice.dto.request;

import com.algotalk.common.pagination.Pagination;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record DashboardRequestDTO(
        @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
        Integer page,

        @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
        @Max(value = 50, message = "페이지 크기는 최대 50까지 가능합니다.")
        Integer size
) {
    public DashboardRequestDTO {
        if (page == null) page = 1;
        if (size == null) size = 5;
    }

    public Pagination toPagination() {
        return Pagination.of(page, size);
    }
}