package com.algotalk.communityservice.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeScrapCommand {

    private Long postId;
    private Long userId;
    private String deletedYn;
    private Long count;         // 집계용
}