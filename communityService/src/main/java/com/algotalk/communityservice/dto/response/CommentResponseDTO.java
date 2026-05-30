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
        String content, // 댓글 내용
        String createdAt, // 댓글 작성 시간
        String updatedAt // 댓글 수정 시간
) {
}
