package com.algotalk.communityservice.controller;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.communityservice.dto.command.ActivityCommand;
import com.algotalk.communityservice.dto.response.MyCommentResponseDTO;
import com.algotalk.communityservice.dto.response.MyLikeResponseDTO;
import com.algotalk.communityservice.dto.response.MyPostResponseDTO;
import com.algotalk.communityservice.dto.response.MyScrapResponseDTO;
import com.algotalk.communityservice.service.ICommunityActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/community/v1/activity")
@RequiredArgsConstructor
public class CommunityActivityController {

    private final ICommunityActivityService communityActivityService;

    // 내가 작성한 게시글 목록
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<List<MyPostResponseDTO>>> getMyPosts(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("{}.getMyPosts Start!", this.getClass().getName());

        List<MyPostResponseDTO> rList = communityActivityService.getMyPosts(
                ActivityCommand.builder()
                        .userId(userId)
                        .page(page)
                        .size(size)
                        .offset((page - 1) * size)
                        .build()
        );

        log.info("{}.getMyPosts End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(rList));
    }

    // 내가 작성한 게시글 삭제
    @DeleteMapping("/posts")
    public ResponseEntity<ApiResponse<Void>> deleteMyPosts(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody List<Long> postIds
    ) {
        log.info("{}.deleteMyPosts Start!", this.getClass().getName());

        communityActivityService.deleteMyPosts(
                ActivityCommand.builder()
                        .userId(userId)
                        .postIds(postIds)
                        .build()
        );

        log.info("{}.deleteMyPosts End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 내가 작성한 댓글 목록
    @GetMapping("/comments")
    public ResponseEntity<ApiResponse<List<MyCommentResponseDTO>>> getMyComments(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("{}.getMyComments Start!", this.getClass().getName());

        List<MyCommentResponseDTO> rList = communityActivityService.getMyComments(
                ActivityCommand.builder()
                        .userId(userId)
                        .page(page)
                        .size(size)
                        .offset((page - 1) * size)
                        .build()
        );

        log.info("{}.getMyComments End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(rList));
    }

    // 내가 작성한 댓글 삭제
    @DeleteMapping("/comments")
    public ResponseEntity<ApiResponse<Void>> deleteMyComments(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody List<Long> commentIds
    ) {
        log.info("{}.deleteMyComments Start!", this.getClass().getName());

        communityActivityService.deleteMyComments(
                ActivityCommand.builder()
                        .userId(userId)
                        .commentIds(commentIds)
                        .build()
        );

        log.info("{}.deleteMyComments End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 내가 스크랩한 게시글 목록
    @GetMapping("/scraps")
    public ResponseEntity<ApiResponse<List<MyScrapResponseDTO>>> getMyScraps(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("{}.getMyScraps Start!", this.getClass().getName());

        List<MyScrapResponseDTO> rList = communityActivityService.getMyScraps(
                ActivityCommand.builder()
                        .userId(userId)
                        .page(page)
                        .size(size)
                        .offset((page - 1) * size)
                        .build()
        );

        log.info("{}.getMyScraps End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(rList));
    }

    // 스크랩 취소
    @DeleteMapping("/scraps")
    public ResponseEntity<ApiResponse<Void>> deleteMyScraps(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody List<Long> postIds
    ) {
        log.info("{}.deleteMyScraps Start!", this.getClass().getName());

        communityActivityService.deleteMyScraps(
                ActivityCommand.builder()
                        .userId(userId)
                        .postIds(postIds)
                        .build()
        );

        log.info("{}.deleteMyScraps End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 내가 좋아요한 게시글 목록
    @GetMapping("/likes")
    public ResponseEntity<ApiResponse<List<MyLikeResponseDTO>>> getMyLikes(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("{}.getMyLikes Start!", this.getClass().getName());

        List<MyLikeResponseDTO> rList = communityActivityService.getMyLikes(
                ActivityCommand.builder()
                        .userId(userId)
                        .page(page)
                        .size(size)
                        .offset((page - 1) * size)
                        .build()
        );

        log.info("{}.getMyLikes End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(rList));
    }

    // 좋아요 취소
    @DeleteMapping("/likes")
    public ResponseEntity<ApiResponse<Void>> deleteMyLikes(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody List<Long> postIds
    ) {
        log.info("{}.deleteMyLikes Start!", this.getClass().getName());

        communityActivityService.deleteMyLikes(
                ActivityCommand.builder()
                        .userId(userId)
                        .postIds(postIds)
                        .build()
        );

        log.info("{}.deleteMyLikes End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}