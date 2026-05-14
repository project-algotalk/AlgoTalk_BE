package com.algotalk.userservice.dto.request;

import lombok.Builder;

@Builder
public record UpdateAddrRequestDTO(
        String addr1,
        String addr2
) {
}
