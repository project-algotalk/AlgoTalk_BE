package com.algotalk.communityservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostListResponseDTO(

    Long postId,
    Long categoryId,
    String categoryCd,
    String categoryName,
    Long userId,
    String nickname,
    String title,
    String contentPreview,      // 앞 100자
    String isNotice,
    Integer viewCount,
    Integer likeCount,
    Integer scrapCount,
    LocalDateTime createdAt,

    List<Long> csCategoryIds,
    List<String> hashtags,

    // 페이징
    Integer totalCount
) {}