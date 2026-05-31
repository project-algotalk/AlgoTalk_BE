package com.algotalk.communityservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.communityservice.client.UserFeignClient;
import com.algotalk.communityservice.dto.response.CsCategoryResponseDTO;
import com.algotalk.communityservice.service.ICsCategoryFeignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.algotalk.communityservice.exception.CommunityErrorCode.CS_CATEGORY_FETCH_FAILED;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsCategoryFeignService implements ICsCategoryFeignService {

    private final UserFeignClient userFeignClient;

    // CS_CATEGORY 전체 목록 조회 (Caffeine 캐시 적용, TTL 30분)
    @Cacheable(cacheNames = "csCategory", key = "'csCategories'")
    public List<CsCategoryResponseDTO> getCategories() {
        log.info("CS_CATEGORY 캐시 미스 - userService 조회");

        try {
            var response = userFeignClient.getCsCategories();

            // userService 응답 null 방어
            if (response == null || response.getData() == null) {
                log.error("userService CS_CATEGORY 응답이 null입니다.");
                throw new BusinessException(CS_CATEGORY_FETCH_FAILED);
            }

            return response.getData();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // 타임아웃/5xx 등 userService 호출 실패
            log.error("userService CS_CATEGORY 조회 실패: {}", e.getMessage(), e);
            throw new BusinessException(CS_CATEGORY_FETCH_FAILED);
        }
    }
}