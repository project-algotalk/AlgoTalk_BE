package com.algotalk.common.pagination;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PaginationRequestDTO(
        @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
        Integer page,

        @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
        @Max(value = 50, message = "페이지 크기는 최대 50까지 가능합니다.")
        Integer size
) {
    public PaginationRequestDTO {
        if (page == null) page = 1;
        if (size == null) size = 10;
    }

    public Pagination toPagination() {
        return Pagination.of(page, size);
    }
}