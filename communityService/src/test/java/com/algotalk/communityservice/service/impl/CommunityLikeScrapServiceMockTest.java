package com.algotalk.communityservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.communityservice.dto.command.LikeScrapCommand;
import com.algotalk.communityservice.dto.response.LikeScrapResponseDTO;
import com.algotalk.communityservice.dto.row.PostDetailRowDTO;
import com.algotalk.communityservice.exception.CommunityErrorCode;
import com.algotalk.communityservice.persistance.IRedisMapper;
import com.algotalk.communityservice.repository.ICommunityLikeScrapMapper;
import com.algotalk.communityservice.repository.ICommunityPostMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommunityLikeScrapServiceMockTest {

    @InjectMocks
    private CommunityLikeScrapService communityLikeScrapService;

    @Mock
    private ICommunityLikeScrapMapper likeScrapMapper;

    @Mock
    private ICommunityPostMapper communityPostMapper;

    @Mock
    private IRedisMapper redisMapper;

    private final Long userId = 1L;
    private final Long postId = 1L;

    private final LikeScrapCommand pCommand = LikeScrapCommand.builder()
            .postId(postId).userId(userId).build();

    private final PostDetailRowDTO mockPost = PostDetailRowDTO.builder()
            .postId(postId).userId(userId).build();

    @Test
    @DisplayName("좋아요 성공 - 최초 좋아요 (Redis 캐시 미스)")
    void toggleLike_success_first_cacheMiss() {
        // given
        given(communityPostMapper.getPostDetail(any())).willReturn(mockPost);
        given(likeScrapMapper.getLike(any())).willReturn(null);  // 최초
        given(redisMapper.getLikeCount(postId)).willReturn(null); // 캐시 미스
        given(likeScrapMapper.getLikeCountFromDB(any())).willReturn(1L);

        // when
        LikeScrapResponseDTO result = communityLikeScrapService.toggleLike(pCommand);

        // then
        assertThat(result.liked()).isTrue();
        assertThat(result.likeCount()).isEqualTo(1L);
        verify(likeScrapMapper).insertLike(any());
        verify(redisMapper).setLikeCount(postId, 1L);
        verify(redisMapper).setUserLiked(postId, userId);
    }

    @Test
    @DisplayName("좋아요 성공 - 최초 좋아요 (Redis 캐시 히트)")
    void toggleLike_success_first_cacheHit() {
        // given
        given(communityPostMapper.getPostDetail(any())).willReturn(mockPost);
        given(likeScrapMapper.getLike(any())).willReturn(null);
        given(redisMapper.getLikeCount(postId)).willReturn(5L); // 캐시 히트
        given(redisMapper.getLikeCount(postId)).willReturn(6L); // increment 후

        // when
        LikeScrapResponseDTO result = communityLikeScrapService.toggleLike(pCommand);

        // then
        assertThat(result.liked()).isTrue();
        verify(likeScrapMapper).insertLike(any());
        verify(redisMapper).incrementLikeCount(postId);
        verify(redisMapper).setUserLiked(postId, userId);
    }

    @Test
    @DisplayName("좋아요 취소 성공")
    void toggleLike_success_cancel() {
        // given
        LikeScrapCommand existing = LikeScrapCommand.builder()
                .postId(postId).userId(userId).deletedYn("N").build();

        given(communityPostMapper.getPostDetail(any())).willReturn(mockPost);
        given(likeScrapMapper.getLike(any())).willReturn(existing);
        given(redisMapper.getLikeCount(postId)).willReturn(3L);
        given(redisMapper.getLikeCount(postId)).willReturn(2L);

        // when
        LikeScrapResponseDTO result = communityLikeScrapService.toggleLike(pCommand);

        // then
        assertThat(result.liked()).isFalse();
        verify(likeScrapMapper).toggleLike(any());
        verify(redisMapper).decrementLikeCount(postId);
        verify(redisMapper).removeUserLiked(postId, userId);
    }

    @Test
    @DisplayName("좋아요 재등록 성공")
    void toggleLike_success_relike() {
        // given
        LikeScrapCommand existing = LikeScrapCommand.builder()
                .postId(postId).userId(userId).deletedYn("Y").build();

        given(communityPostMapper.getPostDetail(any())).willReturn(mockPost);
        given(likeScrapMapper.getLike(any())).willReturn(existing);
        given(redisMapper.getLikeCount(postId)).willReturn(2L);
        given(redisMapper.getLikeCount(postId)).willReturn(3L);

        // when
        LikeScrapResponseDTO result = communityLikeScrapService.toggleLike(pCommand);

        // then
        assertThat(result.liked()).isTrue();
        verify(likeScrapMapper).toggleLike(any());
        verify(redisMapper).incrementLikeCount(postId);
        verify(redisMapper).setUserLiked(postId, userId);
    }

    @Test
    @DisplayName("좋아요 실패 - 게시글 없음")
    void toggleLike_fail_postNotFound() {
        // given
        given(communityPostMapper.getPostDetail(any())).willReturn(null);

        // when & then
        assertThatThrownBy(() -> communityLikeScrapService.toggleLike(pCommand))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommunityErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("스크랩 성공 - 최초 스크랩 (Redis 캐시 미스)")
    void toggleScrap_success_first_cacheMiss() {
        // given
        given(communityPostMapper.getPostDetail(any())).willReturn(mockPost);
        given(likeScrapMapper.getScrap(any())).willReturn(null);
        given(redisMapper.getScrapCount(postId)).willReturn(null);
        given(likeScrapMapper.getScrapCountFromDB(any())).willReturn(1L);

        // when
        LikeScrapResponseDTO result = communityLikeScrapService.toggleScrap(pCommand);

        // then
        assertThat(result.scrapped()).isTrue();
        assertThat(result.scrapCount()).isEqualTo(1L);
        verify(likeScrapMapper).insertScrap(any());
        verify(redisMapper).setScrapCount(postId, 1L);
        verify(redisMapper).setUserScrapped(postId, userId);
    }

    @Test
    @DisplayName("스크랩 성공 - 최초 스크랩 (Redis 캐시 히트)")
    void toggleScrap_success_first_cacheHit() {
        // given
        given(communityPostMapper.getPostDetail(any())).willReturn(mockPost);
        given(likeScrapMapper.getScrap(any())).willReturn(null);
        given(redisMapper.getScrapCount(postId)).willReturn(5L);
        given(redisMapper.getScrapCount(postId)).willReturn(6L);

        // when
        LikeScrapResponseDTO result = communityLikeScrapService.toggleScrap(pCommand);

        // then
        assertThat(result.scrapped()).isTrue();
        verify(likeScrapMapper).insertScrap(any());
        verify(redisMapper).incrementScrapCount(postId);
        verify(redisMapper).setUserScrapped(postId, userId);
    }

    @Test
    @DisplayName("스크랩 취소 성공")
    void toggleScrap_success_cancel() {
        // given
        LikeScrapCommand existing = LikeScrapCommand.builder()
                .postId(postId).userId(userId).deletedYn("N").build();

        given(communityPostMapper.getPostDetail(any())).willReturn(mockPost);
        given(likeScrapMapper.getScrap(any())).willReturn(existing);
        given(redisMapper.getScrapCount(postId)).willReturn(3L);
        given(redisMapper.getScrapCount(postId)).willReturn(2L);

        // when
        LikeScrapResponseDTO result = communityLikeScrapService.toggleScrap(pCommand);

        // then
        assertThat(result.scrapped()).isFalse();
        verify(likeScrapMapper).toggleScrap(any());
        verify(redisMapper).decrementScrapCount(postId);
        verify(redisMapper).removeUserScrapped(postId, userId);
    }

    @Test
    @DisplayName("스크랩 재등록 성공")
    void toggleScrap_success_rescrap() {
        // given
        LikeScrapCommand existing = LikeScrapCommand.builder()
                .postId(postId).userId(userId).deletedYn("Y").build();

        given(communityPostMapper.getPostDetail(any())).willReturn(mockPost);
        given(likeScrapMapper.getScrap(any())).willReturn(existing);
        given(redisMapper.getScrapCount(postId)).willReturn(2L);
        given(redisMapper.getScrapCount(postId)).willReturn(3L);

        // when
        LikeScrapResponseDTO result = communityLikeScrapService.toggleScrap(pCommand);

        // then
        assertThat(result.scrapped()).isTrue();
        verify(likeScrapMapper).toggleScrap(any());
        verify(redisMapper).incrementScrapCount(postId);
        verify(redisMapper).setUserScrapped(postId, userId);
    }

    @Test
    @DisplayName("스크랩 실패 - 게시글 없음")
    void toggleScrap_fail_postNotFound() {
        // given
        given(communityPostMapper.getPostDetail(any())).willReturn(null);

        // when & then
        assertThatThrownBy(() -> communityLikeScrapService.toggleScrap(pCommand))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommunityErrorCode.POST_NOT_FOUND);
    }
}