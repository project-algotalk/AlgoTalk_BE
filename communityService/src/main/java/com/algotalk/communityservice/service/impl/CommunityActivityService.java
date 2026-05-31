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

        List<MyPostResponseDTO> rList = rows.stream()
                .map(row -> {
                    Long likeCount = redisMapper.getLikeCount(row.getPostId());
                    Long scrapCount = redisMapper.getScrapCount(row.getPostId());
                    return MyPostResponseDTO.builder()
                            .postId(row.getPostId())
                            .categoryName(row.getCategoryName())
                            .title(row.getTitle())
                            .nickname(row.getNickname())
                            .likeCount(row.getLikeCount())
                            .scrapCount(row.getScrapCount())
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

        List<MyScrapResponseDTO> rList = rows.stream()
                .map(row -> {
                    Long likeCount = redisMapper.getLikeCount(row.getPostId());
                    Long scrapCount = redisMapper.getScrapCount(row.getPostId());
                    return MyScrapResponseDTO.builder()
                            .postId(row.getPostId())
                            .categoryName(row.getCategoryName())
                            .title(row.getTitle())
                            .nickname(row.getNickname())
                            .likeCount(row.getLikeCount())
                            .scrapCount(row.getScrapCount())
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

        List<MyLikeResponseDTO> rList = rows.stream()
                .map(row -> {
                    Long likeCount = redisMapper.getLikeCount(row.getPostId());
                    Long scrapCount = redisMapper.getScrapCount(row.getPostId());
                    return MyLikeResponseDTO.builder()
                            .postId(row.getPostId())
                            .categoryName(row.getCategoryName())
                            .title(row.getTitle())
                            .nickname(row.getNickname())
                            .likeCount(row.getLikeCount())
                            .scrapCount(row.getScrapCount())
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