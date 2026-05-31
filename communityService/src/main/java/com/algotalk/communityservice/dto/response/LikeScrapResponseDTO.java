package com.algotalk.communityservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LikeScrapResponseDTO(
        Boolean liked,      // 현재 좋아요 여부
        Long likeCount,     // 현재 좋아요 수
        Boolean scrapped,   // 현재 스크랩 여부
        Long scrapCount     // 현재 스크랩 수
) {}