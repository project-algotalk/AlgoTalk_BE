package com.algotalk.communityservice.dto.request;

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
        int page,
        int size
) {
    public PostListRequestDTO {
        if (searchType == null) searchType = "TITLE_CONTENT";
        if (page <= 0) page = 1;
        if (size <= 0) size = 10;
    }
}