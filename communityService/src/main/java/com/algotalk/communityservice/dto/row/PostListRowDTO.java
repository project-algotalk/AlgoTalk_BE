package com.algotalk.communityservice.dto.row;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostListRowDTO {

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
    private String deletedYn;
    private LocalDateTime createdAt;
    private Long csCategoryId;
    private Integer totalCount;     // COUNT(*) OVER() 페이징용
}