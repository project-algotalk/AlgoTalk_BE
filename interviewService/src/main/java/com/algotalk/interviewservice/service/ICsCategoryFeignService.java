package com.algotalk.interviewservice.service;

import com.algotalk.interviewservice.dto.response.CsCategoryResponseDTO;

import java.util.List;

public interface ICsCategoryFeignService {
    List<CsCategoryResponseDTO> getCategories();
}
