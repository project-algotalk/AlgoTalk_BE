package com.algotalk.apigateway.auth.refresh;

import java.util.List;

/**
 * 한 번의 RT 재발급 결과를 leader와 follower 요청이 함께 사용하기 위한 값
 *
 * @param accessToken           원 요청을 다시 보낼 때 사용할 새 Access Token
 * @param setCookies            브라우저에 동일한 새 AT/RT를 저장하기 위한 Set-Cookie 헤더 목록
 */
public record RefreshOutcome(
        String accessToken,
        List<String> setCookies
) {
}