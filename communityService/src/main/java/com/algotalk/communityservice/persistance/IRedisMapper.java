package com.algotalk.communityservice.persistance;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IRedisMapper {

    // 좋아요 수 증가
    void incrementLikeCount(Long postId);

    // 좋아요 수 감소
    void decrementLikeCount(Long postId);

    // 좋아요 수 조회
    Long getLikeCount(Long postId);

    // 좋아요 수 초기화 (DB에서 로드)
    void setLikeCount(Long postId, long count);

    // 좋아요 여부 저장
    void setUserLiked(Long postId, Long userId);

    // 좋아요 취소
    void removeUserLiked(Long postId, Long userId);

    // 좋아요 여부 확인
    boolean isUserLiked(Long postId, Long userId);

    // 스크랩 수 증가
    void incrementScrapCount(Long postId);

    // 스크랩 수 감소
    void decrementScrapCount(Long postId);

    // 스크랩 수 조회
    Long getScrapCount(Long postId);

    // 스크랩 수 초기화 (DB에서 로드)
    void setScrapCount(Long postId, long count);

    // 스크랩 여부 저장
    void setUserScrapped(Long postId, Long userId);

    // 스크랩 취소
    void removeUserScrapped(Long postId, Long userId);

    // 스크랩 여부 확인
    boolean isUserScrapped(Long postId, Long userId);

    // 조회수 증가
    void incrementViewCount(Long postId);

    // 조회수 조회
    Long getViewCount(Long postId);

    // 조회수 초기화 (DB에서 로드)
    void setViewCount(Long postId, long count);

    // 동기화 대상 postId 목록 조회
    Set<String> getViewCountKeys();
    Set<String> getLikeCountKeys();
    Set<String> getScrapCountKeys();

    // 조회수 멀티 조회
    Map<Long, Long> getViewCounts(List<Long> postIds);

    // 좋아요 수 멀티 조회
    Map<Long, Long> getLikeCounts(List<Long> postIds);

    // 스크랩 수 멀티 조회
    Map<Long, Long> getScrapCounts(List<Long> postIds);
}