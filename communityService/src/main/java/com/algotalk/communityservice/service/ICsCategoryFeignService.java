package com.algotalk.communityservice.service;

import com.algotalk.communityservice.dto.response.CsCategoryResponseDTO;

import java.util.List;

public interface ICsCategoryFeignService {
    List<CsCategoryResponseDTO> getCategories();
}
