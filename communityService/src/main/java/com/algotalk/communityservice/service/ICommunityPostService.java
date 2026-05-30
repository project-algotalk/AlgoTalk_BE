package com.algotalk.communityservice.service;

import com.algotalk.communityservice.dto.command.PostCommand;
import com.algotalk.communityservice.dto.command.PostListCommand;
import com.algotalk.communityservice.dto.response.PostDetailResponseDTO;
import com.algotalk.communityservice.dto.response.PostListResponseDTO;

import java.util.List;

public interface ICommunityPostService {
    // 게시글 목록 조회
    List<PostListResponseDTO> getPostList(PostListCommand pCommand);

    // 게시글 상세 조회
    PostDetailResponseDTO getPostDetail(PostCommand pCommand);

    // 게시글 작성
    Long insertPost(PostCommand pCommand);

    // 게시글 수정
    void updatePost(PostCommand pCommand);

    // 게시글 삭제
    void deletePost(PostCommand pCommand);
}
