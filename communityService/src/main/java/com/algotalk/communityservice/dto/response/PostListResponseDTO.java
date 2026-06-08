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
    Integer commentCount,
    LocalDateTime createdAt,

    // CS 카테고리 (Service 레이어에서 캐시 매핑)
    Long csCategoryId,
    String csCategoryName,
    String csCategoryType, // (COMMON_CS / JOB)

    List<String> hashtags,

    // 페이징
    Integer totalCount
) {}