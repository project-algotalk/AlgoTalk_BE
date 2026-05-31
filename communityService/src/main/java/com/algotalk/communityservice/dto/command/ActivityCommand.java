package com.algotalk.communityservice.dto.command;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ActivityCommand {
    private Long userId;
    private List<Long> postIds;
    private List<Long> commentIds;
    private Integer page;
    private Integer size;
    private Integer offset;
}