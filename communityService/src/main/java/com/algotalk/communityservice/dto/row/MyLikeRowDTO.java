package com.algotalk.communityservice.dto.row;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyLikeRowDTO {
    private Long postId;
    private String categoryName;
    private String title;
    private String nickname;
    private Integer likeCount;
    private Integer scrapCount;
    private Integer commentCount;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private Integer totalCount;
}