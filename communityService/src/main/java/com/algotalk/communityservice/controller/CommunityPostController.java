package com.algotalk.communityservice.controller;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.common.response.ApiResponse;
import com.algotalk.communityservice.client.UserFeignClient;
import com.algotalk.communityservice.dto.command.PostCommand;
import com.algotalk.communityservice.dto.command.PostListCommand;
import com.algotalk.communityservice.dto.request.PostListRequestDTO;
import com.algotalk.communityservice.dto.request.PostRequestDTO;
import com.algotalk.communityservice.dto.request.UserInfoRequestDTO;
import com.algotalk.communityservice.dto.response.PostDetailResponseDTO;
import com.algotalk.communityservice.dto.response.PostListResponseDTO;
import com.algotalk.communityservice.dto.response.UserInfoResponseDTO;
import com.algotalk.communityservice.exception.CommunityErrorCode;
import com.algotalk.communityservice.service.ICommunityPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/community/v1/posts")
@RequiredArgsConstructor
public class CommunityPostController {

    private final ICommunityPostService communityPostService;
    private final UserFeignClient userFeignClient;

    // 게시글 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<PostListResponseDTO>>> getPostList(
            @ModelAttribute PostListRequestDTO rDTO
    ) {
        log.info("{}.getPostList Start!", this.getClass().getName());

        PostListCommand pCommand = PostListCommand.builder()
                .categoryId(rDTO.categoryId())
                .categoryCd(rDTO.categoryCd())
                .csCategoryId(rDTO.csCategoryId())
                .keyword(rDTO.keyword())
                .searchType(rDTO.searchType())
                .hashtag(rDTO.hashtag())
                .page(rDTO.page())
                .size(rDTO.size())
                .offset((rDTO.page() - 1) * rDTO.size())
                .build();

        List<PostListResponseDTO> rList = communityPostService.getPostList(pCommand);

        log.info("{}.getPostList End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(rList));
    }

    // 게시글 상세 조회
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponseDTO>> getPostDetail(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long postId
    ) {
        log.info("{}.getPostDetail Start!", this.getClass().getName());

        PostDetailResponseDTO rDTO = communityPostService.getPostDetail(
                PostCommand.builder()
                        .userId(userId)
                        .postId(postId)
                        .build()
        );

        log.info("{}.getPostDetail End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(rDTO));
    }

    // 게시글 작성
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> insertPost(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid PostRequestDTO rDTO
    ) {
        log.info("{}.insertPost Start!", this.getClass().getName());

        // userService에서 닉네임 조회
        String nickname = getNickname(userId);

        PostCommand pCommand = PostCommand.builder()
                .categoryId(rDTO.categoryId())
                .userId(userId)
                .nickname(nickname)
                .title(rDTO.title())
                .content(rDTO.content())
                .csCategoryId(rDTO.csCategoryId())
                .hashtags(rDTO.hashtags())
                .build();

        Long postId = communityPostService.insertPost(pCommand);

        log.info("{}.insertPost End!", this.getClass().getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(postId));
    }

    // 게시글 수정
    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> updatePost(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId,
            @RequestBody @Valid PostRequestDTO rDTO
    ) {
        log.info("{}.updatePost Start!", this.getClass().getName());

        PostCommand pCommand = PostCommand.builder()
                .postId(postId)
                .categoryId(rDTO.categoryId())
                .userId(userId)
                .title(rDTO.title())
                .content(rDTO.content())
                .csCategoryId(rDTO.csCategoryId())
                .hashtags(rDTO.hashtags())
                .build();

        communityPostService.updatePost(pCommand);

        log.info("{}.updatePost End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 게시글 삭제
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId
    ) {
        log.info("{}.deletePost Start!", this.getClass().getName());

        communityPostService.deletePost(
                PostCommand.builder()
                        .postId(postId)
                        .userId(userId)
                        .build()
        );

        log.info("{}.deletePost End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // userService 닉네임 조회 헬퍼
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