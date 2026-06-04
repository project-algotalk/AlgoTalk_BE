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
public class MyCommentRowDTO {
    private Long commentId;
    private Long postId;
    private String categoryName;
    private String nickname;
    private String content;
    private String postTitle;
    private String postDeletedYn;
    private Integer scrapCount;
    private Integer commentCount;
    private Integer viewCount;
    private LocalDateTime updatedAt;
    private Integer totalCount;
}