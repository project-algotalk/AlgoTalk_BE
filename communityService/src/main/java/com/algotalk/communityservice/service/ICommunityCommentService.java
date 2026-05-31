package com.algotalk.communityservice.service;

import com.algotalk.communityservice.dto.command.CommentCommand;
import com.algotalk.communityservice.dto.response.CommentResponseDTO;

import java.util.List;

public interface ICommunityCommentService {

    // 댓글 목록 조회
    List<CommentResponseDTO> getCommentList(CommentCommand pCommand);

    // 댓글 작성
    Long insertComment(CommentCommand pCommand);

    // 댓글 수정
    void updateComment(CommentCommand pCommand);

    // 댓글 삭제
    void deleteComment(CommentCommand pCommand);
}