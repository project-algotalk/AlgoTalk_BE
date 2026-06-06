package com.algotalk.communityservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.communityservice.domain.enums.SortType;
import com.algotalk.communityservice.dto.command.CommentCommand;
import com.algotalk.communityservice.dto.command.HashTagCommand;
import com.algotalk.communityservice.dto.command.LikeScrapCommand;
import com.algotalk.communityservice.dto.command.PostCommand;
import com.algotalk.communityservice.dto.response.CommentResponseDTO;
import com.algotalk.communityservice.persistance.IRedisMapper;
import com.algotalk.communityservice.repository.ICommunityCommentMapper;
import com.algotalk.communityservice.repository.ICommunityHashTagMapper;
import com.algotalk.communityservice.repository.ICommunityLikeScrapMapper;
import com.algotalk.communityservice.repository.ICommunityPostMapper;
import com.algotalk.communityservice.service.ICommunityCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static com.algotalk.communityservice.exception.CommunityErrorCode.*;
import static com.algotalk.communityservice.util.TransactionUtils.runAfterCommit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityCommentService implements ICommunityCommentService {

    private final ICommunityCommentMapper communityCommentMapper;
    private final ICommunityPostMapper communityPostMapper;
    private final ICommunityHashTagMapper communityHashTagMapper;
    private final ICommunityLikeScrapMapper likeScrapMapper;
    private final IRedisMapper redisMapper;

    @Override
    public List<CommentResponseDTO> getCommentList(CommentCommand pCommand) {
        log.info("{}.getCommentList Start!", this.getClass().getName());

        String sortType = SortType.from(pCommand.getSortType()).name();

        List<CommentCommand> rows = communityCommentMapper.getCommentList(
                pCommand.toBuilder()
                        .sortType(sortType)
                        .build()
        );

        List<CommentResponseDTO> rList = rows.stream()
                .map(row -> CommentResponseDTO.builder()
                        .commentId(row.getCommentId())
                        .postId(row.getPostId())
                        .userId(row.getUserId())
                        .nickname(row.getNickname())
                        .content(row.getContent())
                        .parentId(row.getParentId())
                        .groupId(row.getGroupId())
                        .depth(row.getDepth())
                        .deletedYn(row.getDeletedYn())
                        .createdAt(row.getCreatedAt() != null
                                ? row.getCreatedAt().toString() : null)
                        .updatedAt(row.getUpdatedAt() != null
                                ? row.getUpdatedAt().toString() : null)
                        .build())
                .toList();

        log.info("{}.getCommentList End!", this.getClass().getName());
        return rList;
    }

    @Override
    @Transactional
    public Long insertComment(CommentCommand pCommand) {
        log.info("{}.insertComment Start!", this.getClass().getName());

        int depth = 0;
        Long groupId = null;

        if (pCommand.getParentId() != null) {
            // 대댓글: 부모 댓글 조회
            CommentCommand parent = communityCommentMapper.getParentComment(pCommand);

            if (parent == null || "Y".equals(parent.getDeletedYn())) {
                throw new BusinessException(COMMENT_NOT_FOUND);
            }
            if (parent.getDepth() >= 2) {
                throw new BusinessException(COMMENT_DEPTH_EXCEEDED);
            }

            depth = parent.getDepth() + 1;
            groupId = parent.getGroupId();
        }

        CommentCommand insertCommand = CommentCommand.builder()
                .postId(pCommand.getPostId())
                .userId(pCommand.getUserId())
                .nickname(pCommand.getNickname())
                .parentId(pCommand.getParentId())
                .groupId(pCommand.getParentId() == null ? Long.valueOf(0) : groupId)  // 최상위 댓글 임시값 0 (insert 후 자신의 ID로 업데이트)
                .depth(depth)
                .content(pCommand.getContent())
                .build();

        communityCommentMapper.insertComment(insertCommand);
        Long commentId = insertCommand.getCommentId();

        // 최상위 댓글이면 GROUP_ID = 자신의 COMMENT_ID로 업데이트
        if (pCommand.getParentId() == null) {
            CommentCommand updateGroupId = CommentCommand.builder()
                    .commentId(commentId)
                    .groupId(commentId)
                    .userId(pCommand.getUserId())
                    .build();
            communityCommentMapper.updateGroupId(updateGroupId);
        }

        log.info("{}.insertComment End!", this.getClass().getName());
        return commentId;
    }

    @Override
    @Transactional
    public void updateComment(CommentCommand pCommand) {
        log.info("{}.updateComment Start!", this.getClass().getName());

        CommentCommand existing = communityCommentMapper.getComment(pCommand);
        if (existing == null || "Y".equals(existing.getDeletedYn())) {
            throw new BusinessException(COMMENT_NOT_FOUND);
        }
        if (!existing.getUserId().equals(pCommand.getUserId())) {
            throw new BusinessException(COMMENT_UNAUTHORIZED);
        }

        communityCommentMapper.updateComment(pCommand);

        log.info("{}.updateComment End!", this.getClass().getName());
    }

    @Override
    @Transactional
    public void deleteComment(CommentCommand pCommand) {
        log.info("{}.deleteComment Start!", this.getClass().getName());

        CommentCommand existing = communityCommentMapper.getComment(pCommand);
        if (existing == null || "Y".equals(existing.getDeletedYn())) {
            throw new BusinessException(COMMENT_NOT_FOUND);
        }
        if (!existing.getUserId().equals(pCommand.getUserId())) {
            throw new BusinessException(COMMENT_UNAUTHORIZED);
        }

        // 하위 댓글 존재 여부 확인
        boolean hasChildren = communityCommentMapper.hasChildComments(pCommand) > 0;

        if (hasChildren) {
            // 하위 댓글 있으면 소프트딜리트 (내용만 변경)
            communityCommentMapper.softDeleteComment(pCommand);
        } else {
            // 하위 댓글 없으면 하드딜리트
            hardDeleteDeletedCommentsByRootCommentIdFromLeaves(resolveRootCommentId(existing));
            communityCommentMapper.hardDeleteComment(pCommand);
        }

        hardDeleteSoftDeletedRootCommentIfNoActiveGroupComments(resolveRootCommentId(existing));
        hardDeleteSoftDeletedPostIfNoActiveComments(existing.getPostId());

        log.info("{}.deleteComment End!", this.getClass().getName());
    }

    // GROUP_ID가 누락/오염된 이관 데이터도 처리할 수 있도록 PARENT_ID를 따라 최상위 댓글 ID를 찾는다.
    private Long resolveRootCommentId(CommentCommand comment) {
        if (comment == null) {
            return null;
        }

        Long rootCommentId = comment.getCommentId();
        Long parentId = comment.getParentId();
        int guard = 0;

        while (parentId != null && guard++ < 10) {
            CommentCommand parent = communityCommentMapper.getComment(
                    CommentCommand.builder().commentId(parentId).build()
            );
            if (parent == null) {
                break;
            }
            rootCommentId = parent.getCommentId();
            parentId = parent.getParentId();
        }

        if (rootCommentId == null && comment.getGroupId() != null && comment.getGroupId() > 0) {
            return comment.getGroupId();
        }
        return rootCommentId;
    }

    // 소프트딜리트 최상위 댓글에 활성 하위 댓글이 더 이상 없으면 댓글 그룹 하드딜리트
    private void hardDeleteSoftDeletedRootCommentIfNoActiveGroupComments(Long rootCommentId) {
        if (rootCommentId == null) {
            return;
        }

        CommentCommand rootComment = communityCommentMapper.getComment(
                CommentCommand.builder().commentId(rootCommentId).build()
        );
        if (rootComment == null || !"Y".equals(rootComment.getDeletedYn())) {
            return;
        }

        CommentCommand rootCommand = CommentCommand.builder().commentId(rootCommentId).build();
        if (communityCommentMapper.countActiveCommentsByRootCommentId(rootCommand) > 0) {
            return;
        }

        hardDeleteDeletedCommentsByRootCommentIdFromLeaves(rootCommentId);
    }

    // 댓글 그룹 내 삭제된 댓글을 FK 제약에 걸리지 않도록 자식 댓글부터 반복 하드딜리트
    private void hardDeleteDeletedCommentsByRootCommentIdFromLeaves(Long rootCommentId) {
        CommentCommand rootCommand = CommentCommand.builder().commentId(rootCommentId).build();
        int deletedCount;
        do {
            deletedCount = communityCommentMapper.hardDeleteDeletedLeafCommentsByRootCommentId(rootCommand);
        } while (deletedCount > 0);
    }

    // 소프트딜리트 게시글에 활성 댓글이 더 이상 없으면 게시글 및 연관 데이터 하드딜리트
    private void hardDeleteSoftDeletedPostIfNoActiveComments(Long postId) {
        PostCommand postCommand = PostCommand.builder().postId(postId).build();
        if (communityPostMapper.countSoftDeletedPostWithoutActiveComments(postCommand) == 0) {
            return;
        }

        communityHashTagMapper.getPostHashTagIds(
                HashTagCommand.builder().postId(postId).build()
        ).forEach(hashTag -> {
            HashTagCommand command = HashTagCommand.builder()
                    .postId(postId)
                    .hashtagId(hashTag.getHashtagId())
                    .userId(hashTag.getUserId())
                    .build();
            communityHashTagMapper.decrementUseCount(command);
            communityHashTagMapper.deleteUnusedHashTag(command);
        });
        communityHashTagMapper.deletePostHashTags(
                HashTagCommand.builder().postId(postId).build()
        );
        hardDeleteCommentsByPostIdFromLeaves(postId);
        likeScrapMapper.deleteAllLikesByPostId(
                LikeScrapCommand.builder().postId(postId).build()
        );
        likeScrapMapper.deleteAllScrapsByPostId(
                LikeScrapCommand.builder().postId(postId).build()
        );
        communityPostMapper.hardDeletePost(postCommand);
        deletePostCacheAfterCommit(postId);
    }

    private void deletePostCacheAfterCommit(Long postId) {
        if (postId == null) {
            return;
        }

        runAfterCommit(() -> deletePostCache(postId));
    }

    private void deletePostCache(Long postId) {
        try {
            redisMapper.deletePostCache(postId);
        } catch (Exception e) {
            log.warn("게시글 Redis 캐시 삭제 실패: postId={}, error={}", postId, e.getMessage());
        }
    }

    // 게시글의 댓글을 FK 제약에 걸리지 않도록 자식 댓글부터 반복 하드딜리트
    private void hardDeleteCommentsByPostIdFromLeaves(Long postId) {
        CommentCommand commentCommand = CommentCommand.builder().postId(postId).build();
        int deletedCount;
        do {
            deletedCount = communityCommentMapper.hardDeleteLeafCommentsByPostId(commentCommand);
        } while (deletedCount > 0);
    }
}