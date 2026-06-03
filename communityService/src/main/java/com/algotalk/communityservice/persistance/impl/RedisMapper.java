package com.algotalk.communityservice.persistance.impl;

import com.algotalk.communityservice.persistance.IRedisMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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

    private static final long COUNT_TTL_DAYS = 7L;
    private static final long USER_TTL_DAYS = 30L;

    // ====================================================
    //                        좋아요
    // ====================================================
    @Override
    public void incrementLikeCount(Long postId) {
        String key = LIKE_COUNT_KEY + postId;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, COUNT_TTL_DAYS, TimeUnit.DAYS);
    }

    @Override
    public void decrementLikeCount(Long postId) {
        String key = LIKE_COUNT_KEY + postId;
        redisTemplate.opsForValue().decrement(key);
        redisTemplate.expire(key, COUNT_TTL_DAYS, TimeUnit.DAYS);
    }

    @Override
    public Long getLikeCount(Long postId) {
        String value = redisTemplate.opsForValue().get(LIKE_COUNT_KEY + postId);
        return value != null ? Long.parseLong(value) : null;
    }

    @Override
    public void setLikeCount(Long postId, long count) {
        redisTemplate.opsForValue().set(
                LIKE_COUNT_KEY + postId, String.valueOf(count),
                COUNT_TTL_DAYS, TimeUnit.DAYS
        );
    }

    @Override
    public void setUserLiked(Long postId, Long userId) {
        redisTemplate.opsForValue().set(
                LIKE_USER_KEY + postId + ":" + userId, "1",
                USER_TTL_DAYS, TimeUnit.DAYS
        );
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
        String key = SCRAP_COUNT_KEY + postId;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, COUNT_TTL_DAYS, TimeUnit.DAYS);    }

    @Override
    public void decrementScrapCount(Long postId) {
        String key = SCRAP_COUNT_KEY + postId;
        redisTemplate.opsForValue().decrement(key);
        redisTemplate.expire(key, COUNT_TTL_DAYS, TimeUnit.DAYS);    }

    @Override
    public Long getScrapCount(Long postId) {
        String value = redisTemplate.opsForValue().get(SCRAP_COUNT_KEY + postId);
        return value != null ? Long.parseLong(value) : null;
    }

    @Override
    public void setScrapCount(Long postId, long count) {
        redisTemplate.opsForValue().set(
                SCRAP_COUNT_KEY + postId, String.valueOf(count),
                COUNT_TTL_DAYS, TimeUnit.DAYS
        );
    }

    @Override
    public void setUserScrapped(Long postId, Long userId) {
        redisTemplate.opsForValue().set(
                SCRAP_USER_KEY + postId + ":" + userId, "1",
                USER_TTL_DAYS, TimeUnit.DAYS
        );
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
        String key = VIEW_COUNT_KEY + postId;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, COUNT_TTL_DAYS, TimeUnit.DAYS);
    }

    @Override
    public Long getViewCount(Long postId) {
        String value = redisTemplate.opsForValue().get(VIEW_COUNT_KEY + postId);
        return value != null ? Long.parseLong(value) : null;
    }

    @Override
    public void setViewCount(Long postId, long count) {
        redisTemplate.opsForValue().set(
                VIEW_COUNT_KEY + postId, String.valueOf(count),
                COUNT_TTL_DAYS, TimeUnit.DAYS
        );
    }

    // ====================================================
    //                 동기화 대상 키 조회 (SCAN 방식)
    // ====================================================
    @Override
    public Set<String> getViewCountKeys() {
        return scanKeys(VIEW_COUNT_KEY + "*");
    }

    @Override
    public Set<String> getLikeCountKeys() {
        return scanKeys(LIKE_COUNT_KEY + "*");
    }

    @Override
    public Set<String> getScrapCountKeys() {
        return scanKeys(SCRAP_COUNT_KEY + "*");
    }

    private Set<String> scanKeys(String pattern) {
        Set<String> keys = new java.util.HashSet<>();
        org.springframework.data.redis.core.Cursor<String> cursor =
                redisTemplate.scan(
                        org.springframework.data.redis.core.ScanOptions.scanOptions()
                                .match(pattern)
                                .count(100)
                                .build()
                );
        try {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        } finally {
            try {
                cursor.close();
            } catch (Exception e) {
                log.warn("Redis SCAN cursor 닫기 실패: {}", e.getMessage());
            }
        }
        return keys;
    }

    @Override
    public Map<Long, Long> getViewCounts(List<Long> postIds) {
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            postIds.forEach(postId ->
                    connection.stringCommands().get(
                            (VIEW_COUNT_KEY + postId).getBytes(StandardCharsets.UTF_8)
                    )
            );
            return null;
        });

        Map<Long, Long> countMap = new HashMap<>();
        for (int i = 0; i < postIds.size(); i++) {
            Object val = results.get(i);
            if (val != null) {
                String strVal = val instanceof byte[]
                        ? new String((byte[]) val, StandardCharsets.UTF_8)
                        : val.toString();
                try {
                    countMap.put(postIds.get(i), Long.parseLong(strVal));
                } catch (NumberFormatException e) {
                    log.warn("viewCount 파싱 실패: postId={}, val={}", postIds.get(i), strVal);
                }
            }
        }
        return countMap;
    }

    @Override
    public Map<Long, Long> getLikeCounts(List<Long> postIds) {
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            postIds.forEach(postId ->
                    connection.stringCommands().get(
                            (LIKE_COUNT_KEY + postId).getBytes(StandardCharsets.UTF_8)
                    )
            );
            return null;
        });

        Map<Long, Long> countMap = new HashMap<>();
        for (int i = 0; i < postIds.size(); i++) {
            Object val = results.get(i);
            if (val != null) {
                String strVal = val instanceof byte[]
                        ? new String((byte[]) val, StandardCharsets.UTF_8)
                        : val.toString();
                try {
                    countMap.put(postIds.get(i), Long.parseLong(strVal));
                } catch (NumberFormatException e) {
                    log.warn("likeCount 파싱 실패: postId={}, val={}", postIds.get(i), strVal);
                }
            }
        }
        return countMap;
    }

    @Override
    public Map<Long, Long> getScrapCounts(List<Long> postIds) {
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            postIds.forEach(postId ->
                    connection.stringCommands().get(
                            (SCRAP_COUNT_KEY + postId).getBytes(StandardCharsets.UTF_8)
                    )
            );
            return null;
        });

        Map<Long, Long> countMap = new HashMap<>();
        for (int i = 0; i < postIds.size(); i++) {
            Object val = results.get(i);
            if (val != null) {
                String strVal = val instanceof byte[]
                        ? new String((byte[]) val, StandardCharsets.UTF_8)
                        : val.toString();
                try {
                    countMap.put(postIds.get(i), Long.parseLong(strVal));
                } catch (NumberFormatException e) {
                    log.warn("scrapCount 파싱 실패: postId={}, val={}", postIds.get(i), strVal);
                }
            }
        }
        return countMap;
    }
}