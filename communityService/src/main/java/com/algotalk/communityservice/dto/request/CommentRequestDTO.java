package com.algotalk.communityservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CommentRequestDTO(

    @NotBlank(message = "댓글 내용은 필수입니다.")
    String content,

    Long parentId    // null이면 최상위 댓글, 값이 있으면 대댓글
) {}