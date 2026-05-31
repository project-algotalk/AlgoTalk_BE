package com.algotalk.communityservice.repository;

import com.algotalk.communityservice.dto.command.LikeScrapCommand;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ICommunityLikeScrapMapper {

    // 좋아요 조회 (존재 여부 확인)
    LikeScrapCommand getLike(LikeScrapCommand pCommand);

    // 좋아요 등록
    int insertLike(LikeScrapCommand pCommand);

    // 좋아요 토글 (DELETED_YN 변경)
    int toggleLike(LikeScrapCommand pCommand);

    // 스크랩 조회
    LikeScrapCommand getScrap(LikeScrapCommand pCommand);

    // 스크랩 등록
    int insertScrap(LikeScrapCommand pCommand);

    // 스크랩 토글 (DELETED_YN 변경)
    int toggleScrap(LikeScrapCommand pCommand);

    // 게시글 좋아요 수 조회 (DB -> Redis 초기화용)
    Long getLikeCountFromDB(LikeScrapCommand pCommand);

    // 게시글 스크랩 수 조회 (DB -> Redis 초기화용)
    Long getScrapCountFromDB(LikeScrapCommand pCommand);
}