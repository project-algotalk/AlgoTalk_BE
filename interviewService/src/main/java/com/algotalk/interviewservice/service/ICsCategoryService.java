package com.algotalk.interviewservice.service;

import com.algotalk.interviewservice.dto.response.CsCategoryResponseDTO;

import java.util.List;

public interface ICsCategoryService {

    // CS_CATEGORY 전체 목록 조회 (프론트에 카테고리 목록 제공용)
    List<CsCategoryResponseDTO> getCsCategories();

    // categoryId로 카테고리명 조회 (세션 생성 시 categoryId 검증 및 이름 매핑용)
    CsCategoryResponseDTO getCategoryById(Long categoryId);
}