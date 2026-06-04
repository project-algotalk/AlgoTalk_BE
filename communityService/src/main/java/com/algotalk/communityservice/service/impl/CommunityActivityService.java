package com.algotalk.communityservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.communityservice.dto.command.ActivityCommand;
import com.algotalk.communityservice.dto.command.CommentCommand;
import com.algotalk.communityservice.dto.command.HashTagCommand;
import com.algotalk.communityservice.dto.command.LikeScrapCommand;
import com.algotalk.communityservice.dto.command.PostCommand;
import com.algotalk.communityservice.dto.response.MyCommentResponseDTO;
import com.algotalk.communityservice.dto.response.MyLikeResponseDTO;
import com.algotalk.communityservice.dto.response.MyPostResponseDTO;
import com.algotalk.communityservice.dto.response.MyScrapResponseDTO;
import com.algotalk.communityservice.dto.row.MyCommentRowDTO;
import com.algotalk.communityservice.dto.row.MyLikeRowDTO;
import com.algotalk.communityservice.dto.row.MyPostRowDTO;
import com.algotalk.communityservice.dto.row.MyScrapRowDTO;
import com.algotalk.communityservice.persistance.IRedisMapper;
import com.algotalk.communityservice.repository.ICommunityActivityMapper;
import com.algotalk.communityservice.repository.ICommunityCommentMapper;
import com.algotalk.communityservice.repository.ICommunityHashTagMapper;
import com.algotalk.communityservice.repository.ICommunityLikeScrapMapper;
import com.algotalk.communityservice.repository.ICommunityPostMapper;
import com.algotalk.communityservice.service.ICommunityActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.algotalk.communityservice.util.TransactionUtils.runAfterCommit;
import static com.algotalk.communityservice.exception.CommunityErrorCode.INVALID_REQUEST;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityActivityService implements ICommunityActivityService {

    private final ICommunityActivityMapper communityActivityMapper;
    private final ICommunityLikeScrapMapper communityLikeScrapMapper;
    private final ICommunityPostMapper communityPostMapper;
    private final ICommunityCommentMapper communityCommentMapper;
    private final ICommunityHashTagMapper communityHashTagMapper;
    private final IRedisMapper redisMapper;

    @Override
    public List<MyPostResponseDTO> getMyPosts(ActivityCommand pCommand) {
        log.info("{}.getMyPosts Start!", this.getClass().getName());

        List<MyPostRowDTO> rows = communityActivityMapper.getMyPosts(pCommand);
        if (rows.isEmpty()) return List.of();

        List<Long> postIds = rows.stream().map(MyPostRowDTO::getPostId).toList();

        Map<Long, Long> viewCountMap  = redisMapper.getViewCounts(postIds);
        Map<Long, Long> likeCountMap  = redisMapper.getLikeCounts(postIds);
        Map<Long, Long> scrapCountMap = redisMapper.getScrapCounts(postIds);

        List<MyPostResponseDTO> rList = rows.stream()
                .map(row -> {
                    Long postId = row.getPostId();
                    return MyPostResponseDTO.builder()
                            .postId(postId)
                            .categoryName(row.getCategoryName())
                            .title(row.getTitle())
                            .nickname(row.getNickname())
                            .viewCount(viewCountMap.getOrDefault(postId, (long) row.getViewCount()).intValue())
                            .likeCount(likeCountMap.getOrDefault(postId, (long) row.getLikeCount()).intValue())
                            .scrapCount(scrapCountMap.getOrDefault(postId, (long) row.getScrapCount()).intValue())
                            .commentCount(row.getCommentCount())
                            .createdAt(row.getCreatedAt())
                            .totalCount(row.getTotalCount())
                            .build();
                })
                .toList();

        log.info("{}.getMyPosts End!", this.getClass().getName());
        return rList;
    }

    @Override
    @Transactional
    public void deleteMyPosts(ActivityCommand pCommand) {
        log.info("{}.deleteMyPosts Start!", this.getClass().getName());

        if (pCommand.getPostIds() == null || pCommand.getPostIds().isEmpty()) {
            throw new BusinessException(INVALID_REQUEST);
        }

        communityActivityMapper.softDeleteMyPosts(pCommand);
        pCommand.getPostIds().forEach(postId -> {
            deletePostLikesAndScraps(postId);
            deletePostCacheAfterCommit(postId);
        });

        log.info("{}.deleteMyPosts End!", this.getClass().getName());
    }

    @Override
    public List<MyCommentResponseDTO> getMyComments(ActivityCommand pCommand) {
        log.info("{}.getMyComments Start!", this.getClass().getName());

        List<MyCommentRowDTO> rows = communityActivityMapper.getMyComments(pCommand);

        List<MyCommentResponseDTO> rList = rows.stream()
                .map(row -> MyCommentResponseDTO.builder()
                        .commentId(row.getCommentId())
                        .postId(row.getPostId())
                        .categoryName(row.getCategoryName())
                        .nickname(row.getNickname())
                        .content(row.getContent())
                        .postTitle(row.getPostTitle())
                        .postDeletedYn(row.getPostDeletedYn())
                        .scrapCount(row.getScrapCount())
                        .commentCount(row.getCommentCount())
                        .viewCount(row.getViewCount())
                        .updatedAt(row.getUpdatedAt())
                        .totalCount(row.getTotalCount())
                        .build())
                .toList();

        log.info("{}.getMyComments End!", this.getClass().getName());
        return rList;
    }

    @Override
    @Transactional
    public void deleteMyComments(ActivityCommand pCommand) {
        log.info("{}.deleteMyComments Start!", this.getClass().getName());

        if (pCommand.getCommentIds() == null || pCommand.getCommentIds().isEmpty()) {
            throw new BusinessException(INVALID_REQUEST);
        }

        // 내가 작성한 댓글이 포함된 게시글 ID와 최상위 댓글 ID 조회
        List<Long> postIds = communityActivityMapper.getPostIdsByMyComments(pCommand);

        // 내가 작성한 댓글이 포함된 최상위 댓글 ID 조회
        List<Long> rootCommentIds = communityActivityMapper.getRootCommentIdsByMyComments(pCommand);

        // DB에서 댓글 소프트딜리트
        communityActivityMapper.deleteMyComments(pCommand);

        // 소프트딜리트된 최상위 댓글에 활성 하위 댓글이 더 이상 없으면 댓글 그룹 하드딜리트
        rootCommentIds.forEach(this::hardDeleteSoftDeletedRootCommentIfNoActiveGroupComments);
        // 소프트딜리트된 게시글에 활성 댓글이 더 이상 없으면 게시글 및 연관 데이터 하드딜리트
        postIds.forEach(this::hardDeleteSoftDeletedPostIfNoActiveComments);

        log.info("{}.deleteMyComments End!", this.getClass().getName());
    }

    // 소프트딜리트 최상위 댓글에 활성 하위 댓글이 더 이상 없으면 댓글 그룹 하드딜리트
    private void hardDeleteSoftDeletedRootCommentIfNoActiveGroupComments(Long rootCommentId) {
        if (rootCommentId == null) {
            return;
        }

        CommentCommand rootComment = communityCommentMapper.getComment(
                CommentCommand.builder()
                        .commentId(rootCommentId)
                        .build()
        );

        // 최상위 댓글이 존재하지 않거나 이미 하드딜리트된 경우(soft-deleted 상태가 아닌 경우) 하드딜리트 하지 않음
        if (rootComment == null || !"Y".equals(rootComment.getDeletedYn())) {
            return;
        }

        CommentCommand rootCommand = CommentCommand.builder()
                .commentId(rootCommentId)
                .build();

        // 댓글 그룹 내 활성 댓글이 존재하면 하드딜리트 하지 않음
        if (communityCommentMapper.countActiveCommentsByRootCommentId(rootCommand) > 0) {
            return;
        }

        // 댓글 그룹 내 활성 댓글이 더 이상 없으면 댓글 그룹 하드딜리트
        hardDeleteDeletedCommentsByRootCommentIdFromLeaves(rootCommentId);
    }

    // 댓글 그룹 내 삭제된 댓글을 FK 제약에 걸리지 않도록 자식 댓글부터 반복 하드딜리트
    private void hardDeleteDeletedCommentsByRootCommentIdFromLeaves(Long rootCommentId) {
        CommentCommand rootCommand = CommentCommand.builder()
                .commentId(rootCommentId)
                .build();
        int deletedCount;
        do {
            // 댓글 그룹의 삭제된 댓글 중 자식 댓글이 없는 댓글(leaf comment)을 하드딜리트
            deletedCount = communityCommentMapper.hardDeleteDeletedLeafCommentsByRootCommentId(rootCommand);
        } while (deletedCount > 0);
    }

    // 소프트딜리트 게시글에 활성 댓글이 더 이상 없으면 게시글 및 연관 데이터 하드딜리트
    private void hardDeleteSoftDeletedPostIfNoActiveComments(Long postId) {
        PostCommand postCommand = PostCommand.builder()
                .postId(postId)
                .build();

        // 게시글이 소프트딜리트 상태가 아니거나 활성 댓글이 존재하면 하드딜리트 하지 않음
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
        communityLikeScrapMapper.deleteAllLikesByPostId(
                LikeScrapCommand.builder().postId(postId).build()
        );
        communityLikeScrapMapper.deleteAllScrapsByPostId(
                LikeScrapCommand.builder().postId(postId).build()
        );
        communityPostMapper.hardDeletePost(postCommand);
        deletePostCacheAfterCommit(postId);
    }

    private void deletePostLikesAndScraps(Long postId) {
        LikeScrapCommand likeScrapCommand = LikeScrapCommand.builder().postId(postId).build();
        communityLikeScrapMapper.deleteAllLikesByPostId(likeScrapCommand);
        communityLikeScrapMapper.deleteAllScrapsByPostId(likeScrapCommand);
    }

    // 게시글 삭제 후 트랜잭션 커밋 시점에 캐시 삭제 등록
    private void deletePostCacheAfterCommit(Long postId) {
        if (postId == null) {
            return;
        }

        runAfterCommit(() -> deletePostCache(postId));
    }

    // 게시글 캐시 삭제 (예외 발생 시 로그만 남기고 무시)
    private void deletePostCache(Long postId) {
        try {
            // 게시글 상세 캐시 삭제
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
            // 게시글의 댓글 중 자식 댓글이 없는 댓글(leaf comment)을 하드딜리트
            deletedCount = communityCommentMapper.hardDeleteLeafCommentsByPostId(commentCommand);
        } while (deletedCount > 0);
    }


    @Override
    public List<MyScrapResponseDTO> getMyScraps(ActivityCommand pCommand) {
        log.info("{}.getMyScraps Start!", this.getClass().getName());

        List<MyScrapRowDTO> rows = communityActivityMapper.getMyScraps(pCommand);
        if (rows.isEmpty()) return List.of();

        List<Long> postIds = rows.stream().map(MyScrapRowDTO::getPostId).toList();

        Map<Long, Long> viewCountMap  = redisMapper.getViewCounts(postIds);
        Map<Long, Long> likeCountMap  = redisMapper.getLikeCounts(postIds);
        Map<Long, Long> scrapCountMap = redisMapper.getScrapCounts(postIds);

        List<MyScrapResponseDTO> rList = rows.stream()
                .map(row -> {
                    Long postId = row.getPostId();
                    return MyScrapResponseDTO.builder()
                            .postId(postId)
                            .categoryName(row.getCategoryName())
                            .csCategoryId(row.getCsCategoryId())
                            .title(row.getTitle())
                            .nickname(row.getNickname())
                            .viewCount(viewCountMap.getOrDefault(postId, (long) row.getViewCount()).intValue())
                            .likeCount(likeCountMap.getOrDefault(postId, (long) row.getLikeCount()).intValue())
                            .scrapCount(scrapCountMap.getOrDefault(postId, (long) row.getScrapCount()).intValue())
                            .commentCount(row.getCommentCount())
                            .createdAt(row.getCreatedAt())
                            .totalCount(row.getTotalCount())
                            .build();
                })
                .toList();

        log.info("{}.getMyScraps End!", this.getClass().getName());
        return rList;
    }

    @Override
    public List<MyLikeResponseDTO> getMyLikes(ActivityCommand pCommand) {
        log.info("{}.getMyLikes Start!", this.getClass().getName());

        List<MyLikeRowDTO> rows = communityActivityMapper.getMyLikes(pCommand);
        if (rows.isEmpty()) return List.of();

        List<Long> postIds = rows.stream().map(MyLikeRowDTO::getPostId).toList();

        Map<Long, Long> viewCountMap  = redisMapper.getViewCounts(postIds);
        Map<Long, Long> likeCountMap  = redisMapper.getLikeCounts(postIds);
        Map<Long, Long> scrapCountMap = redisMapper.getScrapCounts(postIds);

        List<MyLikeResponseDTO> rList = rows.stream()
                .map(row -> {
                    Long postId = row.getPostId();
                    return MyLikeResponseDTO.builder()
                            .postId(postId)
                            .categoryName(row.getCategoryName())
                            .title(row.getTitle())
                            .nickname(row.getNickname())
                            .viewCount(viewCountMap.getOrDefault(postId, (long) row.getViewCount()).intValue())
                            .likeCount(likeCountMap.getOrDefault(postId, (long) row.getLikeCount()).intValue())
                            .scrapCount(scrapCountMap.getOrDefault(postId, (long) row.getScrapCount()).intValue())
                            .commentCount(row.getCommentCount())
                            .createdAt(row.getCreatedAt())
                            .totalCount(row.getTotalCount())
                            .build();
                })
                .toList();

        log.info("{}.getMyLikes End!", this.getClass().getName());
        return rList;
    }

    @Override
    @Transactional
    public void deleteMyLikes(ActivityCommand pCommand) {
        log.info("{}.deleteMyLikes Start!", this.getClass().getName());

        if (pCommand.getPostIds() == null || pCommand.getPostIds().isEmpty()) {
            throw new BusinessException(INVALID_REQUEST);
        }

        pCommand.getPostIds().forEach(postId ->
                redisMapper.removeUserLiked(postId, pCommand.getUserId())
        );

        communityActivityMapper.deleteMyLikes(pCommand);

        // Redis count 맞추기(DB에서 다시 로드)
        pCommand.getPostIds().forEach(postId -> {
            Long likeCount = communityLikeScrapMapper.getLikeCountFromDB(
                    LikeScrapCommand.builder()
                            .postId(postId)
                            .build()
            );

            likeCount = likeCount != null ? likeCount : 0L;

            redisMapper.setLikeCount(postId, likeCount);
        });

        log.info("{}.deleteMyLikes End!", this.getClass().getName());
    }

    @Override
    @Transactional
    public void deleteMyScraps(ActivityCommand pCommand) {
        log.info("{}.deleteMyScraps Start!", this.getClass().getName());

        if (pCommand.getPostIds() == null || pCommand.getPostIds().isEmpty()) {
            throw new BusinessException(INVALID_REQUEST);
        }

        // Redis 사용자 키 제거
        pCommand.getPostIds().forEach(postId ->
                redisMapper.removeUserScrapped(postId, pCommand.getUserId())
        );

        // DB에서 스크랩 삭제
        communityActivityMapper.deleteMyScraps(pCommand);

        // Redis count 맞추기(DB에서 다시 로드)
        pCommand.getPostIds().forEach(postId -> {
            Long scrapCount = communityLikeScrapMapper.getScrapCountFromDB(
                    LikeScrapCommand.builder()
                            .postId(postId)
                            .build()
            );
            scrapCount = scrapCount != null ? scrapCount : 0L;

            redisMapper.setScrapCount(postId, scrapCount);
        });
        log.info("{}.deleteMyScraps End!", this.getClass().getName());
    }
}