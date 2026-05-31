package com.algotalk.communityservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.communityservice.dto.command.CommentCommand;
import com.algotalk.communityservice.dto.response.CommentResponseDTO;
import com.algotalk.communityservice.exception.CommunityErrorCode;
import com.algotalk.communityservice.repository.ICommunityCommentMapper;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommunityCommentServiceMockTest {

    @InjectMocks
    private CommunityCommentService communityCommentService;

    @Mock
    private ICommunityCommentMapper communityCommentMapper;

    private final Long userId = 1L;
    private final Long postId = 1L;
    private final Long commentId = 1L;

    @Test
    @DisplayName("댓글 목록 조회 성공")
    void getCommentList_success() {
        // given
        CommentCommand row = CommentCommand.builder()
                .commentId(commentId)
                .postId(postId)
                .userId(userId)
                .content("테스트 댓글")
                .depth(0)
                .groupId(commentId)
                .deletedYn("N")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(communityCommentMapper.getCommentList(any())).willReturn(List.of(row));

        // when
        List<CommentResponseDTO> result = communityCommentService.getCommentList(
                CommentCommand.builder().postId(postId).build()
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).commentId()).isEqualTo(commentId);
        assertThat(result.get(0).content()).isEqualTo("테스트 댓글");
    }

    @Test
    @DisplayName("최상위 댓글 작성 성공")
    void insertComment_success_topLevel() {
        // given
        CommentCommand pCommand = CommentCommand.builder()
                .postId(postId)
                .userId(userId)
                .content("최상위 댓글입니다.")
                .build(); // parentId = null

        doAnswer(invocation -> {
            CommentCommand cmd = invocation.getArgument(0);
            ReflectionTestUtils.setField(cmd, "commentId", commentId);
            return 1;
        }).when(communityCommentMapper).insertComment(any());

        // when
        Long result = communityCommentService.insertComment(pCommand);

        // then
        assertThat(result).isEqualTo(commentId);
        verify(communityCommentMapper).updateGroupId(any()); // GROUP_ID 자기 자신으로 업데이트
    }

    @Test
    @DisplayName("대댓글 작성 성공")
    void insertComment_success_reply() {
        // given
        CommentCommand parent = CommentCommand.builder()
                .commentId(commentId)
                .groupId(commentId)
                .depth(0)
                .deletedYn("N")
                .build();

        given(communityCommentMapper.getParentComment(any())).willReturn(parent);

        doAnswer(invocation -> {
            CommentCommand cmd = invocation.getArgument(0);
            ReflectionTestUtils.setField(cmd, "commentId", 2L);
            return 1;
        }).when(communityCommentMapper).insertComment(any());

        CommentCommand pCommand = CommentCommand.builder()
                .postId(postId)
                .userId(userId)
                .parentId(commentId) // 부모 댓글 있음
                .content("대댓글입니다.")
                .build();

        // when
        Long result = communityCommentService.insertComment(pCommand);

        // then
        assertThat(result).isEqualTo(2L);
        verify(communityCommentMapper).insertComment(any());
    }

    @Test
    @DisplayName("대댓글 작성 실패 - 부모 댓글 없음")
    void insertComment_fail_parentNotFound() {
        // given
        given(communityCommentMapper.getParentComment(any())).willReturn(null);

        // when & then
        assertThatThrownBy(() -> communityCommentService.insertComment(
                CommentCommand.builder()
                        .postId(postId)
                        .userId(userId)
                        .parentId(999L)
                        .content("대댓글")
                        .build()
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommunityErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("대댓글 작성 실패 - depth 초과")
    void insertComment_fail_depthExceeded() {
        // given
        CommentCommand parent = CommentCommand.builder()
                .commentId(commentId)
                .groupId(commentId)
                .depth(2) // 이미 최대 depth
                .deletedYn("N")
                .build();

        given(communityCommentMapper.getParentComment(any())).willReturn(parent);

        // when & then
        assertThatThrownBy(() -> communityCommentService.insertComment(
                CommentCommand.builder()
                        .postId(postId)
                        .userId(userId)
                        .parentId(commentId)
                        .content("대대대댓글")
                        .build()
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommunityErrorCode.COMMENT_DEPTH_EXCEEDED);
    }

    @Test
    @DisplayName("댓글 수정 성공")
    void updateComment_success() {
        // given
        CommentCommand existing = CommentCommand.builder()
                .commentId(commentId)
                .userId(userId)
                .deletedYn("N")
                .build();

        given(communityCommentMapper.getComment(any())).willReturn(existing);

        // when
        communityCommentService.updateComment(
                CommentCommand.builder()
                        .commentId(commentId)
                        .userId(userId)
                        .content("수정된 댓글")
                        .build()
        );

        // then
        verify(communityCommentMapper).updateComment(any());
    }

    @Test
    @DisplayName("댓글 수정 실패 - 존재하지 않는 댓글")
    void updateComment_fail_notFound() {
        // given
        given(communityCommentMapper.getComment(any())).willReturn(null);

        // when & then
        assertThatThrownBy(() -> communityCommentService.updateComment(
                CommentCommand.builder()
                        .commentId(commentId)
                        .userId(userId)
                        .content("수정된 댓글")
                        .build()
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommunityErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("댓글 수정 실패 - 작성자 불일치")
    void updateComment_fail_unauthorized() {
        // given
        CommentCommand existing = CommentCommand.builder()
                .commentId(commentId)
                .userId(9999L)
                .deletedYn("N")
                .build();

        given(communityCommentMapper.getComment(any())).willReturn(existing);

        // when & then
        assertThatThrownBy(() -> communityCommentService.updateComment(
                CommentCommand.builder()
                        .commentId(commentId)
                        .userId(userId)
                        .content("수정된 댓글")
                        .build()
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommunityErrorCode.COMMENT_UNAUTHORIZED);
    }

    @Test
    @DisplayName("댓글 삭제 성공")
    void deleteComment_success() {
        // given
        CommentCommand existing = CommentCommand.builder()
                .commentId(commentId)
                .userId(userId)
                .deletedYn("N")
                .build();

        given(communityCommentMapper.getComment(any())).willReturn(existing);

        // when
        communityCommentService.deleteComment(
                CommentCommand.builder()
                        .commentId(commentId)
                        .userId(userId)
                        .build()
        );

        // then
        verify(communityCommentMapper).deleteComment(any());
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 존재하지 않는 댓글")
    void deleteComment_fail_notFound() {
        // given
        given(communityCommentMapper.getComment(any())).willReturn(null);

        // when & then
        assertThatThrownBy(() -> communityCommentService.deleteComment(
                CommentCommand.builder()
                        .commentId(commentId)
                        .userId(userId)
                        .build()
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommunityErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 작성자 불일치")
    void deleteComment_fail_unauthorized() {
        // given
        CommentCommand existing = CommentCommand.builder()
                .commentId(commentId)
                .userId(9999L)
                .deletedYn("N")
                .build();

        given(communityCommentMapper.getComment(any())).willReturn(existing);

        // when & then
        assertThatThrownBy(() -> communityCommentService.deleteComment(
                CommentCommand.builder()
                        .commentId(commentId)
                        .userId(userId)
                        .build()
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommunityErrorCode.COMMENT_UNAUTHORIZED);
    }
}