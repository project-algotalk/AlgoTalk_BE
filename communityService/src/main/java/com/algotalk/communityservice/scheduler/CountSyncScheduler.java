package com.algotalk.communityservice.scheduler;

import com.algotalk.communityservice.dto.command.PostCommand;
import com.algotalk.communityservice.persistance.IRedisMapper;
import com.algotalk.communityservice.repository.ICommunityPostMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class CountSyncScheduler {

    private final ICommunityPostMapper communityPostMapper;
    private final IRedisMapper redisMapper;

    private static final String VIEW_COUNT_KEY  = "community:view:count:";
    private static final String LIKE_COUNT_KEY  = "community:like:count:";
    private static final String SCRAP_COUNT_KEY = "community:scrap:count:";

    // 5분마다 실행
    @Scheduled(fixedDelay = 300000)
    public void syncCountsToDB() {
        log.info("{}.syncCountsToDB Start!", this.getClass().getName());

        syncViewCounts();
        syncLikeCounts();
        syncScrapCounts();

        log.info("{}.syncCountsToDB End!", this.getClass().getName());
    }

    private void syncViewCounts() {
        Set<String> keys = redisMapper.getViewCountKeys();
        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            try {
                Long postId = Long.parseLong(key.replace(VIEW_COUNT_KEY, ""));
                Long viewCount = redisMapper.getViewCount(postId);
                if (viewCount != null) {
                    communityPostMapper.syncViewCount(
                            PostCommand.builder()
                                    .postId(postId)
                                    .viewCount(viewCount.intValue())
                                    .build()
                    );
                    log.info("viewCount 동기화 완료: postId={}, count={}", postId, viewCount);
                }
            } catch (Exception e) {
                log.error("viewCount 동기화 실패: key={}, error={}", key, e.getMessage());
            }
        }
    }

    private void syncLikeCounts() {
        Set<String> keys = redisMapper.getLikeCountKeys();
        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            try {
                Long postId = Long.parseLong(key.replace(LIKE_COUNT_KEY, ""));
                Long likeCount = redisMapper.getLikeCount(postId);
                if (likeCount != null) {
                    communityPostMapper.syncLikeCount(
                            PostCommand.builder()
                                    .postId(postId)
                                    .likeCount(likeCount.intValue())
                                    .build()
                    );
                    log.info("likeCount 동기화 완료: postId={}, count={}", postId, likeCount);
                }
            } catch (Exception e) {
                log.error("likeCount 동기화 실패: key={}, error={}", key, e.getMessage());
            }
        }
    }

    private void syncScrapCounts() {
        Set<String> keys = redisMapper.getScrapCountKeys();
        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            try {
                Long postId = Long.parseLong(key.replace(SCRAP_COUNT_KEY, ""));
                Long scrapCount = redisMapper.getScrapCount(postId);
                if (scrapCount != null) {
                    communityPostMapper.syncScrapCount(
                            PostCommand.builder()
                                    .postId(postId)
                                    .scrapCount(scrapCount.intValue())
                                    .build()
                    );
                    log.info("scrapCount 동기화 완료: postId={}, count={}", postId, scrapCount);
                }
            } catch (Exception e) {
                log.error("scrapCount 동기화 실패: key={}, error={}", key, e.getMessage());
            }
        }
    }
}