package com.algotalk.userservice.auth.oauth2;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final String AUTHORIZATION_BASE_URI = "/oauth2/authorization";
    private static final String LINK_TOKEN_PARAM = "linkToken";
    private static final String LINK_TOKEN_PREFIX = "oauth2:link:";
    private static final String NONCE_PREFIX = "oauth2:nonce:";
    private static final String STATE_LINK_SEPARATOR = "::LINK::";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, AUTHORIZATION_BASE_URI);
        return customize(request, resolver.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, AUTHORIZATION_BASE_URI);
        return customize(request, resolver.resolve(request, clientRegistrationId));
    }

    private OAuth2AuthorizationRequest customize(HttpServletRequest request, OAuth2AuthorizationRequest req) {
        if (req == null) {
            return null;
        }

        String linkToken = request.getParameter(LINK_TOKEN_PARAM);
        if (linkToken == null || linkToken.isBlank()) {
            return req;
        }

        Boolean exists = redisTemplate.hasKey(LINK_TOKEN_PREFIX + linkToken);
        if (!Boolean.TRUE.equals(exists)) {
            return req;
        }

        String nonce = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(NONCE_PREFIX + nonce, linkToken, 5, TimeUnit.MINUTES);

        String compositeState = req.getState() + STATE_LINK_SEPARATOR + nonce;
        return OAuth2AuthorizationRequest.from(req)
                .state(compositeState)
                .build();
    }
}