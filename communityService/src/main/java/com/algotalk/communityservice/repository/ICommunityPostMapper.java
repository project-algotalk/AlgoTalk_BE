package com.algotalk.communityservice.repository;

import com.algotalk.communityservice.dto.command.PostCommand;
import com.algotalk.communityservice.dto.command.PostListCommand;
import com.algotalk.communityservice.dto.response.PostDetailResponseDTO;
import com.algotalk.communityservice.dto.row.PostDetailRowDTO;
import com.algotalk.communityservice.dto.row.PostListRowDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ICommunityPostMapper {

    // 게시글 목록 조회 (카테고리별, 페이징)
    List<PostListRowDTO> getPostList(PostListCommand pCommand);

    // 게시글 상세
    PostDetailRowDTO getPostDetail(PostCommand pCommand);

    // 게시글 작성
    int insertPost(PostCommand pCommand);

    // 게시글 수정
    int updatePost(PostCommand pCommand);

    // 게시글 소프트딜리트
    int deletePost(PostCommand pCommand);

    // 조회수 Redis -> DB 동기화
    int syncViewCount(PostCommand pCommand);

    // 좋아요수 Redis -> DB 동기화
    int syncLikeCount(PostCommand pCommand);

    // 스크랩수 Redis -> DB 동기화
    int syncScrapCount(PostCommand pCommand);

    Long getViewCount(Long postId);
}
