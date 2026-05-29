package com.algotalk.communityservice.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
public record UserInfoRequestDTO(
        Long userId
) {}