package com.algotalk.communityservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MyCommentResponseDTO(
        Long commentId,
        Long postId,
        String categoryName,
        String nickname,
        String content,
        String postTitle,
        Integer scrapCount,
        Integer commentCount,
        LocalDateTime createdAt,
        Integer totalCount
) {}