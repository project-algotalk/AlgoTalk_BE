package com.algotalk.communityservice.repository;

import com.algotalk.communityservice.dto.command.LikeScrapCommand;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ICommunityLikeScrapMapper {

    // 좋아요 조회
    LikeScrapCommand getLike(LikeScrapCommand pCommand);

    // 좋아요 등록
    int insertLike(LikeScrapCommand pCommand);

    // 좋아요 삭제 (하드딜리트)
    int deleteLike(LikeScrapCommand pCommand);

    // 스크랩 조회
    LikeScrapCommand getScrap(LikeScrapCommand pCommand);

    // 스크랩 등록
    int insertScrap(LikeScrapCommand pCommand);

    // 스크랩 삭제 (하드딜리트)
    int deleteScrap(LikeScrapCommand pCommand);

    // 좋아요 수 조회 (DB)
    Long getLikeCountFromDB(LikeScrapCommand pCommand);

    // 스크랩 수 조회 (DB)
    Long getScrapCountFromDB(LikeScrapCommand pCommand);
}