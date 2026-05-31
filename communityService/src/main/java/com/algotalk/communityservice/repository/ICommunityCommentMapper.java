package com.algotalk.communityservice.repository;

import com.algotalk.communityservice.dto.command.CommentCommand;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ICommunityCommentMapper {

    // 댓글 목록 조회 (게시글 번호 postId 기준)
    List<CommentCommand> getCommentList(CommentCommand pCommand);

    // 댓글 단건 조회 (존재/작성자 확인용)
    CommentCommand getComment(CommentCommand pCommand);

    // 부모 댓글 조회 (대댓글 작성 시 depth, groupId 계산용)
    CommentCommand getParentComment(CommentCommand pCommand);

    // 댓글 작성
    int insertComment(CommentCommand pCommand);

    // 댓글 수정
    int updateComment(CommentCommand pCommand);

    // 댓글 groupId 업데이트 (자신의 commentId로 groupId 업데이트)
    int updateGroupId(CommentCommand pCommand);

    // 댓글 소프트딜리트
    int deleteComment(CommentCommand pCommand);
}