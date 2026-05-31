package com.algotalk.communityservice.persistance.impl;

import com.algotalk.communityservice.persistance.IRedisMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class RedisMapper implements IRedisMapper {

    private final StringRedisTemplate redisTemplate;

    private static final String LIKE_COUNT_KEY   = "community:like:count:";
    private static final String LIKE_USER_KEY    = "community:like:user:";
    private static final String SCRAP_COUNT_KEY  = "community:scrap:count:";
    private static final String SCRAP_USER_KEY   = "community:scrap:user:";
    private static final String VIEW_COUNT_KEY   = "community:view:count:";

    // ====================================================
    //                        좋아요
    // ====================================================
    @Override
    public void incrementLikeCount(Long postId) {
        redisTemplate.opsForValue().increment(LIKE_COUNT_KEY + postId);
    }

    @Override
    public void decrementLikeCount(Long postId) {
        redisTemplate.opsForValue().decrement(LIKE_COUNT_KEY + postId);
    }

    @Override
    public Long getLikeCount(Long postId) {
        String value = redisTemplate.opsForValue().get(LIKE_COUNT_KEY + postId);
        return value != null ? Long.parseLong(value) : null;
    }

    @Override
    public void setLikeCount(Long postId, long count) {
        redisTemplate.opsForValue().set(LIKE_COUNT_KEY + postId, String.valueOf(count));
    }

    @Override
    public void setUserLiked(Long postId, Long userId) {
        redisTemplate.opsForValue().set(LIKE_USER_KEY + postId + ":" + userId, "1");
    }

    @Override
    public void removeUserLiked(Long postId, Long userId) {
        redisTemplate.delete(LIKE_USER_KEY + postId + ":" + userId);
    }

    @Override
    public boolean isUserLiked(Long postId, Long userId) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(LIKE_USER_KEY + postId + ":" + userId)
        );
    }

    // ====================================================
    //                        스크랩
    // ====================================================
    @Override
    public void incrementScrapCount(Long postId) {
        redisTemplate.opsForValue().increment(SCRAP_COUNT_KEY + postId);
    }

    @Override
    public void decrementScrapCount(Long postId) {
        redisTemplate.opsForValue().decrement(SCRAP_COUNT_KEY + postId);
    }

    @Override
    public Long getScrapCount(Long postId) {
        String value = redisTemplate.opsForValue().get(SCRAP_COUNT_KEY + postId);
        return value != null ? Long.parseLong(value) : null;
    }

    @Override
    public void setScrapCount(Long postId, long count) {
        redisTemplate.opsForValue().set(SCRAP_COUNT_KEY + postId, String.valueOf(count));
    }

    @Override
    public void setUserScrapped(Long postId, Long userId) {
        redisTemplate.opsForValue().set(SCRAP_USER_KEY + postId + ":" + userId, "1");
    }

    @Override
    public void removeUserScrapped(Long postId, Long userId) {
        redisTemplate.delete(SCRAP_USER_KEY + postId + ":" + userId);
    }

    @Override
    public boolean isUserScrapped(Long postId, Long userId) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(SCRAP_USER_KEY + postId + ":" + userId)
        );
    }


    // ====================================================
    //                        조회수
    // ====================================================
    @Override
    public void incrementViewCount(Long postId) {
        redisTemplate.opsForValue().increment(VIEW_COUNT_KEY + postId);
    }

    @Override
    public Long getViewCount(Long postId) {
        String value = redisTemplate.opsForValue().get(VIEW_COUNT_KEY + postId);
        return value != null ? Long.parseLong(value) : null;
    }

    @Override
    public void setViewCount(Long postId, long count) {
        redisTemplate.opsForValue().set(VIEW_COUNT_KEY + postId, String.valueOf(count));
    }
}