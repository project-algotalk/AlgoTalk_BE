package com.algotalk.communityservice.dto.request;

import com.algotalk.common.pagination.Pagination;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

import java.util.List;

@Builder
public record PostListRequestDTO(
        Long categoryId,
        String categoryCd,
        Long csCategoryId,
        List<Long> csCategoryIds,
        String keyword,
        String searchType,
        String hashtag,

        @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
        Integer page,

        @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
        @Max(value = 50, message = "페이지 크기는 최대 50까지 가능합니다.")
        Integer size
) {
    public PostListRequestDTO {
        if (searchType == null) searchType = "TITLE_CONTENT";
        if (page == null) page = 1;
        if (size == null) size = 10;
    }

    public Pagination toPagination() {
        return Pagination.of(page, size);
    }
}