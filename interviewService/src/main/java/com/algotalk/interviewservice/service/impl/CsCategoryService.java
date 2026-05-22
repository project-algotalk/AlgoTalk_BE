package com.algotalk.interviewservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.interviewservice.dto.response.CsCategoryResponseDTO;
import com.algotalk.interviewservice.service.ICsCategoryFeignService;
import com.algotalk.interviewservice.service.ICsCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.algotalk.interviewservice.exception.InterviewErrorCode.INVALID_CATEGORY_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsCategoryService implements ICsCategoryService {

    private final ICsCategoryFeignService csCategoryFeignService;

    @Override
    public List<CsCategoryResponseDTO> getCsCategories() {
        log.info("{}.getCsCategories Start!", this.getClass().getName());

        List<CsCategoryResponseDTO> rDTO = csCategoryFeignService.getCategories();

        log.info("{}.getCsCategories End!", this.getClass().getName());
        return rDTO;
    }

    @Override
    public CsCategoryResponseDTO getCategoryById(Long categoryId) {
        log.info("{}.getCategoryById Start!", this.getClass().getName());

        // 캐시된 목록에서 categoryId로 조회 (별도 빈을 통해 캐시 적용)
        CsCategoryResponseDTO rDTO = csCategoryFeignService.getCategories().stream()
                .filter(c -> c.categoryId().equals(categoryId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(INVALID_CATEGORY_ID));

        log.info("{}.getCategoryById End!", this.getClass().getName());
        return rDTO;
    }
}