package com.algotalk.communityservice.dto.command;

import lombok.Builder;
import lombok.Getter;

@Builder
public class HashTagCommand {

    private Long postId;
    private Long hashtagId;
    private String tagName;
    private Long userId;

}