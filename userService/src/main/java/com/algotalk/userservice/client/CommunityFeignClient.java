package com.algotalk.userservice.client;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.userservice.dto.response.MyCommentResponseDTO;
import com.algotalk.userservice.dto.response.MyLikeResponseDTO;
import com.algotalk.userservice.dto.response.MyPostResponseDTO;
import com.algotalk.userservice.dto.response.MyScrapResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "COMMUNITY-SERVICE")
public interface CommunityFeignClient {

    // 내가 작성한 게시글 목록
    @GetMapping("/community/v1/activity/posts")
    ApiResponse<List<MyPostResponseDTO>> getMyPosts(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    );

    // 내가 작성한 게시글 삭제
    @DeleteMapping("/community/v1/activity/posts")
    ApiResponse<Void> deleteMyPosts(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody List<Long> postIds
    );

    // 내가 작성한 댓글 목록
    @GetMapping("/community/v1/activity/comments")
    ApiResponse<List<MyCommentResponseDTO>> getMyComments(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    );

    // 내가 작성한 댓글 삭제
    @DeleteMapping("/community/v1/activity/comments")
    ApiResponse<Void> deleteMyComments(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody List<Long> commentIds
    );

    // 내가 스크랩한 게시글 목록
    @GetMapping("/community/v1/activity/scraps")
    ApiResponse<List<MyScrapResponseDTO>> getMyScraps(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    );

    // 스크랩 취소
    @DeleteMapping("/community/v1/activity/scraps")
    ApiResponse<Void> deleteMyScraps(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody List<Long> postIds
    );

    // 내가 좋아요한 게시글 목록
    @GetMapping("/community/v1/activity/likes")
    ApiResponse<List<MyLikeResponseDTO>> getMyLikes(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    );

    // 좋아요 취소
    @DeleteMapping("/community/v1/activity/likes")
    ApiResponse<Void> deleteMyLikes(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody List<Long> postIds
    );
}