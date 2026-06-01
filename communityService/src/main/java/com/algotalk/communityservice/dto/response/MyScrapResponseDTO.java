package com.algotalk.communityservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MyScrapResponseDTO(
        Long postId,
        String categoryName,
        String title,
        String nickname,
        Integer likeCount,
        Integer scrapCount,
        Integer commentCount,
        Integer viewCount,
        LocalDateTime createdAt,
        Integer totalCount
) {}