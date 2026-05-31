package com.algotalk.communityservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommentResponseDTO(
        Long commentId, // 댓글 ID
        Long postId, // 게시글 ID
        Long userId, // 작성자 ID
        String nickname, // 작성자 닉네임
        Long parentId, // 부모 댓글 ID (대댓글인 경우)
        Long groupId, // 댓글 그룹 ID (대댓글 그룹핑용)
        Integer depth, // 댓글 깊이 (0: 일반 댓글, 1: 대댓글)
        String content, // 댓글 내용
        String deletedYn, // 삭제 여부 (Y/N)
        String createdAt, // 댓글 작성 시간
        String updatedAt // 댓글 수정 시간
) {
}
