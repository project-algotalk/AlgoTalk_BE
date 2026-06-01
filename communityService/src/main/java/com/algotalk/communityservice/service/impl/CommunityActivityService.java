package com.algotalk.communityservice.service.impl;

import com.algotalk.communityservice.dto.command.ActivityCommand;
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
import com.algotalk.communityservice.service.ICommunityActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityActivityService implements ICommunityActivityService {

    private final ICommunityActivityMapper communityActivityMapper;
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
        communityActivityMapper.deleteMyPosts(pCommand);
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
                        .scrapCount(row.getScrapCount())
                        .commentCount(row.getCommentCount())
                        .viewCount(row.getViewCount())
                        .createdAt(row.getCreatedAt())
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
        communityActivityMapper.deleteMyComments(pCommand);
        log.info("{}.deleteMyComments End!", this.getClass().getName());
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
        pCommand.getPostIds().forEach(postId ->
                redisMapper.removeUserLiked(postId, pCommand.getUserId())
        );
        communityActivityMapper.deleteMyLikes(pCommand);
        log.info("{}.deleteMyLikes End!", this.getClass().getName());
    }

    @Override
    @Transactional
    public void deleteMyScraps(ActivityCommand pCommand) {
        log.info("{}.deleteMyScraps Start!", this.getClass().getName());
        pCommand.getPostIds().forEach(postId ->
                redisMapper.removeUserScrapped(postId, pCommand.getUserId())
        );
        communityActivityMapper.deleteMyScraps(pCommand);
        log.info("{}.deleteMyScraps End!", this.getClass().getName());
    }
}