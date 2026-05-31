package com.algotalk.userservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExistsResponseDTO(
        // 가상 컬럼
        /**
         * DB 테이블에 존재하지 않는 가상의 컬럼
         * 회원 가입시 중복 체크 용으로 사용(Y/N)
         */
        String existsYn
) {
}
