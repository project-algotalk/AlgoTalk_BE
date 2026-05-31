package com.algotalk.communityservice.controller;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.communityservice.dto.command.LikeScrapCommand;
import com.algotalk.communityservice.dto.response.LikeScrapResponseDTO;
import com.algotalk.communityservice.service.ICommunityLikeScrapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/community/v1/posts")
@RequiredArgsConstructor
public class CommunityLikeScrapController {

    private final ICommunityLikeScrapService communityLikeScrapService;

    // 좋아요 토글
    @PostMapping("/{postId}/likes")
    public ResponseEntity<ApiResponse<LikeScrapResponseDTO>> toggleLike(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId
    ) {
        log.info("{}.toggleLike Start!", this.getClass().getName());

        LikeScrapResponseDTO rDTO = communityLikeScrapService.toggleLike(
                LikeScrapCommand.builder()
                        .postId(postId)
                        .userId(userId)
                        .build()
        );

        log.info("{}.toggleLike End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(rDTO));
    }

    // 스크랩 토글
    @PostMapping("/{postId}/scraps")
    public ResponseEntity<ApiResponse<LikeScrapResponseDTO>> toggleScrap(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId
    ) {
        log.info("{}.toggleScrap Start!", this.getClass().getName());

        LikeScrapResponseDTO rDTO = communityLikeScrapService.toggleScrap(
                LikeScrapCommand.builder()
                        .postId(postId)
                        .userId(userId)
                        .build()
        );

        log.info("{}.toggleScrap End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(rDTO));
    }
}