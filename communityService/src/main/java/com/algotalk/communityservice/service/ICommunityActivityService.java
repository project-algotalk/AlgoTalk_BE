package com.algotalk.communityservice.service;

import com.algotalk.communityservice.dto.command.ActivityCommand;
import com.algotalk.communityservice.dto.response.MyCommentResponseDTO;
import com.algotalk.communityservice.dto.response.MyLikeResponseDTO;
import com.algotalk.communityservice.dto.response.MyPostResponseDTO;
import com.algotalk.communityservice.dto.response.MyScrapResponseDTO;

import java.util.List;

public interface ICommunityActivityService {

    // 내가 작성한 게시글 목록
    List<MyPostResponseDTO> getMyPosts(ActivityCommand pCommand);

    // 내가 작성한 게시글 삭제
    void deleteMyPosts(ActivityCommand pCommand);

    // 내가 작성한 댓글 목록
    List<MyCommentResponseDTO> getMyComments(ActivityCommand pCommand);

    // 내가 작성한 댓글 삭제
    void deleteMyComments(ActivityCommand pCommand);

    // 내가 스크랩한 게시글 목록
    List<MyScrapResponseDTO> getMyScraps(ActivityCommand pCommand);

    // 내가 좋아요한 게시글 목록
    List<MyLikeResponseDTO> getMyLikes(ActivityCommand pCommand);

    // 스크랩 취소
    void deleteMyScraps(ActivityCommand pCommand);

    // 좋아요 취소
    void deleteMyLikes(ActivityCommand pCommand);
}