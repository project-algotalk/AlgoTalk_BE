package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.command.SocialAccountCommand;
import com.algotalk.userservice.dto.request.SocialLinkRequestDTO;
import com.algotalk.userservice.repository.ISocialAccountMapper;
import com.algotalk.userservice.service.ISocialLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.algotalk.userservice.exception.UserErrorCode.SOCIAL_ALREADY_LINKED_OTHER;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialLinkService implements ISocialLinkService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ISocialAccountMapper socialAccountMapper;

    private static final String LINK_TOKEN_PREFIX = "oauth2:link:";
    private static final long LINK_TOKEN_TTL_MIN = 5L;

    @Override
    public String issueLinkToken(SocialLinkRequestDTO pDTO) throws Exception {
        log.info("{}.issueLinkToken Start!", this.getClass().getName());

        String linkToken = UUID.randomUUID().toString();

        Map<String, String> data = new HashMap<>();
        data.put("userId", pDTO.userId().toString());
        data.put("provider", pDTO.provider().toUpperCase());

        redisTemplate.opsForHash().putAll(LINK_TOKEN_PREFIX + linkToken, data);
        redisTemplate.expire(LINK_TOKEN_PREFIX + linkToken, LINK_TOKEN_TTL_MIN, TimeUnit.MINUTES);


        log.info("{}.issueLinkToken End!", this.getClass().getName());
        return linkToken;
    }

    @Transactional
    @Override
    public void linkSocialAccount(SocialLinkRequestDTO pDTO) throws Exception {
        log.info("{}.linkSocialAccount Start!", this.getClass().getName());

        SocialAccountCommand pCommand = SocialAccountCommand.builder()
                .userId(pDTO.userId())
                .provider(pDTO.provider())
                .providerId(pDTO.providerId())
                .build();

        // 이미 내 계정에 연결된 경우
        if (socialAccountMapper.existsByUserIdAndProvider(pCommand)) {
            log.info("이미 연결된 소셜 계정");
            return;
        }

        // 다른 계정에 연결되어 있는 경우
        try {
            socialAccountMapper.insertSocialAccount(pCommand);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(SOCIAL_ALREADY_LINKED_OTHER);
        }

        log.info("{}.linkSocialAccount End!", this.getClass().getName());
    }
}
