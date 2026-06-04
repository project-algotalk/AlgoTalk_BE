package com.algotalk.communityservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.communityservice.dto.command.LikeScrapCommand;
import com.algotalk.communityservice.dto.response.LikeScrapResponseDTO;
import com.algotalk.communityservice.exception.CommunityErrorCode;
import com.algotalk.communityservice.persistance.IRedisMapper;
import com.algotalk.communityservice.repository.ICommunityLikeScrapMapper;
import com.algotalk.communityservice.repository.ICommunityPostMapper;
import com.algotalk.communityservice.dto.command.PostCommand;
import com.algotalk.communityservice.dto.row.PostDetailRowDTO;
import com.algotalk.communityservice.service.ICommunityLikeScrapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityLikeScrapService implements ICommunityLikeScrapService {

    private final ICommunityLikeScrapMapper likeScrapMapper;
    private final ICommunityPostMapper communityPostMapper;
    private final IRedisMapper redisMapper;

    @Override
    @Transactional
    public LikeScrapResponseDTO toggleLike(LikeScrapCommand pCommand) {
        log.info("{}.toggleLike Start!", this.getClass().getName());

        // 게시글 존재 확인
        PostDetailRowDTO post = communityPostMapper.getPostDetail(
                PostCommand.builder().postId(pCommand.getPostId()).build()
        );
        if (post == null) throw new BusinessException(CommunityErrorCode.POST_NOT_FOUND);

        // DB 좋아요 처리
        LikeScrapCommand existing = likeScrapMapper.getLike(pCommand);
        boolean liked;

        if (existing == null) {
            // 좋아요 등록
            likeScrapMapper.insertLike(pCommand);
            liked = true;
        } else {
            // 좋아요 취소
            likeScrapMapper.hardDeleteLike(pCommand);
            liked = false;
        }

        // Redis 좋아요 수 처리
        Long likeCount = redisMapper.getLikeCount(pCommand.getPostId());
        if (likeCount == null) {
            likeCount = likeScrapMapper.getLikeCountFromDB(pCommand);
            redisMapper.setLikeCount(pCommand.getPostId(), likeCount);
        } else {
            if (liked) redisMapper.incrementLikeCount(pCommand.getPostId());
            else redisMapper.decrementLikeCount(pCommand.getPostId());
            likeCount = redisMapper.getLikeCount(pCommand.getPostId());
        }

        // Redis 좋아요 여부 처리
        if (liked) redisMapper.setUserLiked(pCommand.getPostId(), pCommand.getUserId());
        else redisMapper.removeUserLiked(pCommand.getPostId(), pCommand.getUserId());

        log.info("{}.toggleLike End!", this.getClass().getName());
        return LikeScrapResponseDTO.builder()
                .liked(liked)
                .likeCount(likeCount)
                .build();
    }

    @Override
    @Transactional
    public LikeScrapResponseDTO toggleScrap(LikeScrapCommand pCommand) {
        log.info("{}.toggleScrap Start!", this.getClass().getName());

        // 게시글 존재 확인
        PostDetailRowDTO post = communityPostMapper.getPostDetail(
                PostCommand.builder().postId(pCommand.getPostId()).build()
        );
        if (post == null) throw new BusinessException(CommunityErrorCode.POST_NOT_FOUND);

        // 스크랩 허용 여부 확인
        if (!"Y".equals(post.getIsScrapable())) {
            throw new BusinessException(CommunityErrorCode.SCRAP_NOT_ALLOWED);
        }

        // DB 스크랩 처리
        LikeScrapCommand existing = likeScrapMapper.getScrap(pCommand);
        boolean scrapped;

        if (existing == null) {
            // 스크랩 등록
            likeScrapMapper.insertScrap(pCommand);
            scrapped = true;
        } else {
            // 스크랩 취소
            likeScrapMapper.hardDeleteScrap(pCommand);
            scrapped = false;
        }

        // Redis 스크랩 수 처리
        Long scrapCount = redisMapper.getScrapCount(pCommand.getPostId());
        if (scrapCount == null) {
            scrapCount = likeScrapMapper.getScrapCountFromDB(pCommand);
            redisMapper.setScrapCount(pCommand.getPostId(), scrapCount);
        } else {
            if (scrapped) redisMapper.incrementScrapCount(pCommand.getPostId());
            else redisMapper.decrementScrapCount(pCommand.getPostId());
            scrapCount = redisMapper.getScrapCount(pCommand.getPostId());
        }

        // Redis 스크랩 여부 처리
        if (scrapped) redisMapper.setUserScrapped(pCommand.getPostId(), pCommand.getUserId());
        else redisMapper.removeUserScrapped(pCommand.getPostId(), pCommand.getUserId());

        log.info("{}.toggleScrap End!", this.getClass().getName());
        return LikeScrapResponseDTO.builder()
                .scrapped(scrapped)
                .scrapCount(scrapCount)
                .build();
    }
}