package com.algotalk.userservice.service.impl;

import com.algotalk.userservice.dto.response.CsCategoryResponseDTO;
import com.algotalk.userservice.repository.ICsCategoryMapper;
import com.algotalk.userservice.service.ICsCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsCategoryService implements ICsCategoryService {

    private final ICsCategoryMapper csCategoryMapper;

    @Override
    public List<CsCategoryResponseDTO> getCsCategories() throws Exception {
        log.info("{}.getCsCategories Start!", this.getClass().getName());

        List<CsCategoryResponseDTO> rList = csCategoryMapper.getCsCategories().stream()
                .map(c -> CsCategoryResponseDTO.builder()
                        .categoryId(c.getCategoryId())
                        .categoryType(c.getCategoryType())
                        .categoryName(c.getCategoryName())
                        .parentId(c.getParentId())
                        .depth(c.getDepth())
                        .sortOrder(c.getSortOrder())
                        .build())
                .toList();

        log.info("{}.getCsCategories End!", this.getClass().getName());
        return rList;
    }
}
