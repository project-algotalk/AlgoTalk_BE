package com.algotalk.interviewservice.client;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.interviewservice.dto.response.CsCategoryResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "USER-SERVICE")
public interface UserFeignClient {

    // userService CS_CATEGORY 전체 목록 조회
    @GetMapping("/cs-categories/v1")
    ApiResponse<List<CsCategoryResponseDTO>> getCsCategories();
}