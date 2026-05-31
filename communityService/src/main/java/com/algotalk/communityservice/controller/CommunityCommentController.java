package com.algotalk.communityservice.controller;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.common.response.ApiResponse;
import com.algotalk.communityservice.client.UserFeignClient;
import com.algotalk.communityservice.dto.command.CommentCommand;
import com.algotalk.communityservice.dto.request.CommentRequestDTO;
import com.algotalk.communityservice.dto.request.UserInfoRequestDTO;
import com.algotalk.communityservice.dto.response.CommentResponseDTO;
import com.algotalk.communityservice.dto.response.UserInfoResponseDTO;
import com.algotalk.communityservice.exception.CommunityErrorCode;
import com.algotalk.communityservice.service.ICommunityCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/community/v1/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommunityCommentController {

    private final ICommunityCommentService communityCommentService;
    private final UserFeignClient userFeignClient;

    // 댓글 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentResponseDTO>>> getCommentList(
            @PathVariable Long postId
    ) {
        log.info("{}.getCommentList Start!", this.getClass().getName());

        List<CommentResponseDTO> rList = communityCommentService.getCommentList(
                CommentCommand.builder().postId(postId).build()
        );

        log.info("{}.getCommentList End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(rList));
    }

    // 댓글 작성
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> insertComment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId,
            @RequestBody @Valid CommentRequestDTO rDTO
    ) {
        log.info("{}.insertComment Start!", this.getClass().getName());
        // userService에서 닉네임 조회
        String nickname = getNickname(userId);

        CommentCommand pCommand = CommentCommand.builder()
                .postId(postId)
                .userId(userId)
                .nickname(nickname)
                .parentId(rDTO.parentId())
                .content(rDTO.content())
                .build();

        Long commentId = communityCommentService.insertComment(pCommand);

        log.info("{}.insertComment End!", this.getClass().getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(commentId));
    }

    // 댓글 수정
    @PutMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> updateComment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody @Valid CommentRequestDTO rDTO
    ) {
        log.info("{}.updateComment Start!", this.getClass().getName());

        CommentCommand pCommand = CommentCommand.builder()
                .commentId(commentId)
                .postId(postId)
                .userId(userId)
                .content(rDTO.content())
                .build();

        communityCommentService.updateComment(pCommand);

        log.info("{}.updateComment End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        log.info("{}.deleteComment Start!", this.getClass().getName());

        communityCommentService.deleteComment(
                CommentCommand.builder()
                        .commentId(commentId)
                        .postId(postId)
                        .userId(userId)
                        .build()
        );

        log.info("{}.deleteComment End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private String getNickname(Long userId) {
        ApiResponse<UserInfoResponseDTO> response = userFeignClient.getNicknameByUserId(
                UserInfoRequestDTO.builder().userId(userId).build()
        );
        if (response == null || response.getData() == null) {
            throw new BusinessException(CommunityErrorCode.UNAUTHORIZED);
        }
        return response.getData().nickname();
    }
}