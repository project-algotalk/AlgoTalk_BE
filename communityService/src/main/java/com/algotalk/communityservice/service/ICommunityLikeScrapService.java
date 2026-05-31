package com.algotalk.communityservice.service;

import com.algotalk.communityservice.dto.command.LikeScrapCommand;
import com.algotalk.communityservice.dto.response.LikeScrapResponseDTO;

public interface ICommunityLikeScrapService {

    // 좋아요 토글
    LikeScrapResponseDTO toggleLike(LikeScrapCommand pCommand);

    // 스크랩 토글
    LikeScrapResponseDTO toggleScrap(LikeScrapCommand pCommand);
}