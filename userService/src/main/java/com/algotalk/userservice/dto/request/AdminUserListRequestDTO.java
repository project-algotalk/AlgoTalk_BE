package com.algotalk.userservice.dto.request;

import com.algotalk.common.pagination.Pagination;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AdminUserListRequestDTO(
        @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
        Integer page,

        @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
        @Max(value = 50, message = "페이지 크기는 최대 50까지 가능합니다.")
        Integer size,

        @Size(max = 50, message = "검색어는 50자 이하여야 합니다.")
        String keyword
) {
    public Pagination toPagination() {
        return Pagination.of(
                page == null ? 1 : page,
                size == null ? 10 : size
        );
    }

    public String normalizedKeyword() {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim();
    }
}
