package com.algotalk.communityservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.communityservice.dto.command.CommentCommand;
import com.algotalk.communityservice.dto.response.CommentResponseDTO;
import com.algotalk.communityservice.repository.ICommunityCommentMapper;
import com.algotalk.communityservice.service.ICommunityCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.algotalk.communityservice.exception.CommunityErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityCommentService implements ICommunityCommentService {

    private final ICommunityCommentMapper communityCommentMapper;

    @Override
    public List<CommentResponseDTO> getCommentList(CommentCommand pCommand) {
        log.info("{}.getCommentList Start!", this.getClass().getName());

        List<CommentCommand> rows = communityCommentMapper.getCommentList(pCommand);

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
            communityCommentMapper.deleteComment(pCommand);
        } else {
            // 하위 댓글 없으면 하드딜리트
            communityCommentMapper.hardDeleteComment(pCommand);
        }

        log.info("{}.deleteComment End!", this.getClass().getName());
    }
}