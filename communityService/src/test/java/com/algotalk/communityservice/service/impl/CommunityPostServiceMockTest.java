package com.algotalk.communityservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.common.pagination.Pagination;
import com.algotalk.communityservice.client.AiFeignClient;
import com.algotalk.communityservice.dto.command.HashTagCommand;
import com.algotalk.communityservice.dto.command.PostCommand;
import com.algotalk.communityservice.dto.command.PostListCommand;
import com.algotalk.communityservice.dto.request.CsValidationRequestDTO;
import com.algotalk.communityservice.dto.response.CsCategoryResponseDTO;
import com.algotalk.communityservice.dto.response.CsValidationItemDTO;
import com.algotalk.communityservice.dto.response.CsValidationResponseDTO;
import com.algotalk.communityservice.dto.response.PostDetailResponseDTO;
import com.algotalk.communityservice.dto.response.PostListResponseDTO;
import com.algotalk.communityservice.dto.row.PostDetailRowDTO;
import com.algotalk.communityservice.dto.row.PostListRowDTO;
import com.algotalk.communityservice.exception.CommunityErrorCode;
import com.algotalk.communityservice.persistance.IRedisMapper;
import com.algotalk.communityservice.repository.ICommunityHashTagMapper;
import com.algotalk.communityservice.repository.ICommunityPostMapper;
import com.algotalk.communityservice.service.ICsCategoryFeignService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityPostServiceMockTest {

    @InjectMocks
    private CommunityPostService communityPostService;

    @Mock
    private ICommunityPostMapper communityPostMapper;

    @Mock
    private ICommunityHashTagMapper communityHashTagMapper;

    @Mock
    private ICsCategoryFeignService csCategoryFeignService;

    @Mock
    private IRedisMapper redisMapper;

    @Mock
    private AiFeignClient aiFeignClient;

    private final Long userId = 1L;
    private final Long postId = 1L;
    private final Long categoryId = 1L;
    private final Long csCategoryId = 101L;

    private CsValidationResponseDTO mockValidResponse(String title) {
        return new CsValidationResponseDTO(
                List.of(new CsValidationItemDTO(title, true, null))
        );
    }

    @Test
    @DisplayName("게시글 목록 조회 성공 - 해시태그 있음")
    void getPostList_success_withHashtags() {
        // given
        given(redisMapper.getLikeCount(any())).willReturn(0L);
        given(redisMapper.getScrapCount(any())).willReturn(0L);
        given(redisMapper.getViewCount(any())).willReturn(0L);
        given(communityHashTagMapper.getPostHashTags(any())).willReturn(
                List.of(HashTagCommand.builder().tagName("스프링").build())
        );
        given(csCategoryFeignService.getCategories()).willReturn(
                List.of(new CsCategoryResponseDTO(101L, "JOB", "백엔드 개발자", null, 1, 1))
        );

        PostListRowDTO row = PostListRowDTO.builder()
                .postId(postId)
                .categoryId(categoryId)
                .categoryCd("QUESTION")
                .categoryName("질문공유")
                .userId(userId)
                .nickname("테스트유저")
                .title("테스트 제목")
                .content("테스트 내용입니다.")
                .isNotice("N")
                .viewCount(0)
                .likeCount(0)
                .scrapCount(0)
                .csCategoryId(csCategoryId)
                .createdAt(LocalDateTime.now())
                .totalCount(1)
                .build();

        given(communityPostMapper.getPostList(any())).willReturn(List.of(row));

        // when
        List<PostListResponseDTO> result = communityPostService.getPostList(
                PostListCommand.builder().categoryId(categoryId).pagination(Pagination.of(1, 10)).build()
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).postId()).isEqualTo(postId);
        assertThat(result.get(0).hashtags()).containsExactly("스프링");
        assertThat(result.get(0).csCategoryName()).isEqualTo("백엔드 개발자");
    }

    @Test
    @DisplayName("게시글 목록 조회 성공 - 결과 없음")
    void getPostList_success_empty() {
        // given
        given(communityPostMapper.getPostList(any())).willReturn(List.of());

        // when
        List<PostListResponseDTO> result = communityPostService.getPostList(
                PostListCommand.builder().categoryId(categoryId).pagination(Pagination.of(1, 10)).build()
        );

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("게시글 상세 조회 성공")
    void getPostDetail_success() {
        // given
        given(redisMapper.getLikeCount(any())).willReturn(0L);
        given(redisMapper.getScrapCount(any())).willReturn(0L);
        given(redisMapper.getViewCount(any())).willReturn(0L);
        given(redisMapper.isUserLiked(any(), any())).willReturn(false);
        given(redisMapper.isUserScrapped(any(), any())).willReturn(false);
        given(communityHashTagMapper.getPostHashTags(any())).willReturn(List.of());
        given(csCategoryFeignService.getCategories()).willReturn(
                List.of(new CsCategoryResponseDTO(101L, "JOB", "백엔드 개발자", null, 1, 1))
        );

        PostDetailRowDTO mockRow = PostDetailRowDTO.builder()
                .postId(postId)
                .categoryId(categoryId)
                .categoryCd("QUESTION")
                .categoryName("질문공유")
                .userId(userId)
                .nickname("테스트유저")
                .title("테스트 제목")
                .content("테스트 내용입니다.")
                .isNotice("N")
                .viewCount(0)
                .likeCount(0)
                .scrapCount(0)
                .csCategoryId(csCategoryId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(communityPostMapper.getPostDetail(any())).willReturn(mockRow);

        // when
        PostDetailResponseDTO result = communityPostService.getPostDetail(
                PostCommand.builder().postId(postId).userId(userId).build()
        );

        // then
        assertThat(result.postId()).isEqualTo(postId);
        assertThat(result.csCategoryName()).isEqualTo("백엔드 개발자");
        assertThat(result.csCategoryType()).isEqualTo("JOB");
    }

    @Test
    @DisplayName("게시글 상세 조회 실패 - 존재하지 않는 게시글")
    void getPostDetail_fail_notFound() {
        // given
        given(communityPostMapper.getPostDetail(any())).willReturn(null);

        // when & then
        assertThatThrownBy(() -> communityPostService.getPostDetail(
                PostCommand.builder().postId(postId).userId(userId).build()
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommunityErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("게시글 작성 성공 - 해시태그 있음")
    void insertPost_success_withHashtags() {
        // given
        given(aiFeignClient.validateCsQuestions(any())).willReturn(
                mockValidResponse("테스트 제목")
        );

        PostCommand pCommand = PostCommand.builder()
                .categoryId(categoryId)
                .userId(userId)
                .nickname("테스트유저")
                .title("테스트 제목")
                .content("테스트 내용입니다.")
                .csCategoryId(csCategoryId)
                .hashtags(List.of("스프링", "백엔드"))
                .build();

        doAnswer(invocation -> {
            PostCommand cmd = invocation.getArgument(0);
            ReflectionTestUtils.setField(cmd, "postId", postId);
            return 1;
        }).when(communityPostMapper).insertPost(any());

        given(communityHashTagMapper.getHashTagId(any())).willReturn(null);
        doAnswer(invocation -> {
            HashTagCommand cmd = invocation.getArgument(0);
            ReflectionTestUtils.setField(cmd, "hashtagId", 1L);
            return 1;
        }).when(communityHashTagMapper).insertHashTag(any());

        // when
        Long result = communityPostService.insertPost(pCommand);

        // then
        assertThat(result).isEqualTo(postId);
        verify(communityHashTagMapper, times(2)).insertHashTag(any());
        verify(communityHashTagMapper, times(2)).insertPostHashTag(any());
    }

    @Test
    @DisplayName("게시글 작성 성공 - 해시태그 없음")
    void insertPost_success_noHashtags() {
        // given
        given(aiFeignClient.validateCsQuestions(any())).willReturn(
                mockValidResponse("테스트 제목")
        );

        PostCommand pCommand = PostCommand.builder()
                .categoryId(categoryId)
                .userId(userId)
                .nickname("테스트유저")
                .title("테스트 제목")
                .content("테스트 내용입니다.")
                .build();

        doAnswer(invocation -> {
            PostCommand cmd = invocation.getArgument(0);
            ReflectionTestUtils.setField(cmd, "postId", postId);
            return 1;
        }).when(communityPostMapper).insertPost(any());

        // when
        Long result = communityPostService.insertPost(pCommand);

        // then
        assertThat(result).isEqualTo(postId);
        verify(communityHashTagMapper, never()).insertHashTag(any());
        verify(communityHashTagMapper, never()).insertPostHashTag(any());
    }

    @Test
    @DisplayName("게시글 수정 성공")
    void updatePost_success() {
        // given
        PostDetailRowDTO existing = PostDetailRowDTO.builder()
                .postId(postId).userId(userId).build();

        given(communityPostMapper.getPostDetail(any())).willReturn(existing);
        given(aiFeignClient.validateCsQuestions(any())).willReturn(
                mockValidResponse("수정된 제목")
        );
        given(communityHashTagMapper.getHashTagId(any())).willReturn(null);
        doAnswer(invocation -> {
            HashTagCommand cmd = invocation.getArgument(0);
            ReflectionTestUtils.setField(cmd, "hashtagId", 2L);
            return 1;
        }).when(communityHashTagMapper).insertHashTag(any());

        PostCommand pCommand = PostCommand.builder()
                .postId(postId)
                .userId(userId)
                .categoryId(categoryId)
                .title("수정된 제목")
                .content("수정된 내용")
                .hashtags(List.of("수정태그"))
                .build();

        // when
        communityPostService.updatePost(pCommand);

        // then
        verify(communityPostMapper).updatePost(any());
        verify(communityHashTagMapper).deletePostHashTags(any());
        verify(communityHashTagMapper).insertHashTag(any());
    }

    @Test
    @DisplayName("게시글 수정 실패 - 존재하지 않는 게시글")
    void updatePost_fail_notFound() {
        // given
        given(communityPostMapper.getPostDetail(any())).willReturn(null);

        // when & then
        assertThatThrownBy(() -> communityPostService.updatePost(
                PostCommand.builder().postId(postId).userId(userId).build()
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommunityErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("게시글 수정 실패 - 작성자 불일치")
    void updatePost_fail_unauthorized() {
        // given
        PostDetailRowDTO existing = PostDetailRowDTO.builder()
                .postId(postId).userId(9999L).build();

        given(communityPostMapper.getPostDetail(any())).willReturn(existing);

        // when & then
        assertThatThrownBy(() -> communityPostService.updatePost(
                PostCommand.builder().postId(postId).userId(userId).build()
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommunityErrorCode.POST_UNAUTHORIZED);
    }

    @Test
    @DisplayName("게시글 삭제 성공")
    void deletePost_success() {
        // given
        PostDetailRowDTO existing = PostDetailRowDTO.builder()
                .postId(postId).userId(userId).build();

        given(communityPostMapper.getPostDetail(any())).willReturn(existing);

        // when
        communityPostService.deletePost(
                PostCommand.builder().postId(postId).userId(userId).build()
        );

        // then
        verify(communityPostMapper).deletePost(any());
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 존재하지 않는 게시글")
    void deletePost_fail_notFound() {
        // given
        given(communityPostMapper.getPostDetail(any())).willReturn(null);

        // when & then
        assertThatThrownBy(() -> communityPostService.deletePost(
                PostCommand.builder().postId(postId).userId(userId).build()
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommunityErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 작성자 불일치")
    void deletePost_fail_unauthorized() {
        // given
        PostDetailRowDTO existing = PostDetailRowDTO.builder()
                .postId(postId).userId(9999L).build();

        given(communityPostMapper.getPostDetail(any())).willReturn(existing);

        // when & then
        assertThatThrownBy(() -> communityPostService.deletePost(
                PostCommand.builder().postId(postId).userId(userId).build()
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommunityErrorCode.POST_UNAUTHORIZED);
    }
}