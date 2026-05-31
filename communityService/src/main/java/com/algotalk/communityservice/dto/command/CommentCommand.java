package com.algotalk.communityservice.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentCommand {

    private Long commentId;
    private Long postId;
    private Long userId;
    private String nickname;
    private Long parentId;      // null이면 최상위 댓글
    private Long groupId;       // 최상위 COMMENT_ID
    private Integer depth;      // 0: 댓글, 1: 대댓글, 2: 대대댓글
    private String content;
    private String deletedYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}