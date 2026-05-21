package com.algotalk.interviewservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.interviewservice.client.UserFeignClient;
import com.algotalk.interviewservice.dto.response.CsCategoryResponseDTO;
import com.algotalk.interviewservice.service.ICsCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.algotalk.interviewservice.exception.InterviewErrorCode.INVALID_CATEGORY_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsCategoryService implements ICsCategoryService {

    private final UserFeignClient userFeignClient;

    @Cacheable(cacheNames = "csCategory", key = "'csCategories'")
    @Override
    public List<CsCategoryResponseDTO> getCsCategories() {
        log.info("{}.getCsCategories Start!", this.getClass().getName());

        List<CsCategoryResponseDTO> rDTO = userFeignClient.getCsCategories().getData();

        log.info("{}.getCsCategories End!", this.getClass().getName());
        return rDTO;
    }

    @Override
    public CsCategoryResponseDTO getCategoryById(Long categoryId) {
        log.info("{}.getCategoryById Start!", this.getClass().getName());

        // 캐시된 목록에서 categoryId로 조회
        CsCategoryResponseDTO rDTO = getCsCategories().stream()
                .filter(c -> c.categoryId().equals(categoryId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(INVALID_CATEGORY_ID));

        log.info("{}.getCategoryById End!", this.getClass().getName());
        return rDTO;
    }
}