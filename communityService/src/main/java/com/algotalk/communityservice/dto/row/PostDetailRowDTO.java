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
public class PostDetailRowDTO {

    private Long postId;
    private Long categoryId;
    private String categoryCd;
    private String categoryName;
    private Long userId;
    private String nickname;
    private String title;
    private String content;
    private String isNotice;
    private Integer viewCount;
    private Integer likeCount;
    private Integer scrapCount;
    private Integer commentCount;
    private Long csCategoryId;
    private String isScrapable;
    private String deletedYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}