package com.algotalk.communityservice.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HashTagCommand {

    private Long postId;
    private Long hashtagId;
    private String tagName;
    private Long userId;

}