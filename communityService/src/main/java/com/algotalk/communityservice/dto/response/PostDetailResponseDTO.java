package com.algotalk.communityservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostDetailResponseDTO(

    Long postId,
    Long categoryId,
    String categoryCd,
    String categoryName,
    Long userId,
    String nickname,
    String title,
    String content,
    String isNotice,
    Integer viewCount,
    Integer likeCount,
    Integer scrapCount,
    Integer commentCount,
    Boolean liked,              // 현재 사용자 좋아요 여부
    Boolean scrapped,           // 현재 사용자 스크랩 여부
    LocalDateTime createdAt,
    LocalDateTime updatedAt,

    // CS 카테고리
    Long csCategoryId,
    String csCategoryName,
    String csCategoryType,

    List<String> hashtags,
    List<CommentResponseDTO> comments
) {}