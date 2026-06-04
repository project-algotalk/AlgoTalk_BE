package com.algotalk.communityservice.repository;

import com.algotalk.communityservice.dto.command.ActivityCommand;
import com.algotalk.communityservice.dto.row.MyCommentRowDTO;
import com.algotalk.communityservice.dto.row.MyLikeRowDTO;
import com.algotalk.communityservice.dto.row.MyPostRowDTO;
import com.algotalk.communityservice.dto.row.MyScrapRowDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ICommunityActivityMapper {

    // 내가 작성한 게시글 목록
    List<MyPostRowDTO> getMyPosts(ActivityCommand pCommand);

    // 내가 작성한 게시글 소프트딜리트
    int softDeleteMyPosts(ActivityCommand pCommand);

    // 내가 작성한 댓글 목록
    List<MyCommentRowDTO> getMyComments(ActivityCommand pCommand);

    // 내가 작성한 댓글 삭제
    int deleteMyComments(ActivityCommand pCommand);

    // 내가 작성한 댓글이 속한 게시글 ID 목록
    List<Long> getPostIdsByMyComments(ActivityCommand pCommand);

    // 내가 작성한 댓글이 속한 최상위 댓글 ID 목록
    List<Long> getRootCommentIdsByMyComments(ActivityCommand pCommand);

    // 내가 스크랩한 게시글 목록
    List<MyScrapRowDTO> getMyScraps(ActivityCommand pCommand);

    // 내가 좋아요한 게시글 목록
    List<MyLikeRowDTO> getMyLikes(ActivityCommand pCommand);

    // 스크랩 취소
    int deleteMyScraps(ActivityCommand pCommand);

    // 좋아요 취소
    int deleteMyLikes(ActivityCommand pCommand);
}