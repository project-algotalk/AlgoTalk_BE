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
    int hardDeleteLike(LikeScrapCommand pCommand);

    // 게시글의 좋아요 전체 삭제 (하드딜리트)
    int deleteAllLikesByPostId(LikeScrapCommand pCommand);

    // 스크랩 조회
    LikeScrapCommand getScrap(LikeScrapCommand pCommand);

    // 스크랩 등록
    int insertScrap(LikeScrapCommand pCommand);

    // 스크랩 삭제 (하드딜리트)
    int hardDeleteScrap(LikeScrapCommand pCommand);

    // 게시글의 스크랩 전체 삭제 (하드딜리트)
    int deleteAllScrapsByPostId(LikeScrapCommand pCommand);

    // 좋아요 수 조회 (DB)
    Long getLikeCountFromDB(LikeScrapCommand pCommand);

    // 스크랩 수 조회 (DB)
    Long getScrapCountFromDB(LikeScrapCommand pCommand);

}