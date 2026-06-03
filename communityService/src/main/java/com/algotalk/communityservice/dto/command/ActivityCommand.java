package com.algotalk.communityservice.dto.command;

import com.algotalk.common.pagination.Pagination;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ActivityCommand {
    private Long userId;
    private List<Long> postIds;
    private List<Long> commentIds;
    private Pagination pagination;    // 페이지네이션 정보 (page, size, offset)
}