package com.algotalk.communityservice.client;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.communityservice.dto.request.UserInfoRequestDTO;
import com.algotalk.communityservice.dto.response.CsCategoryResponseDTO;
import com.algotalk.communityservice.dto.response.UserInfoResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "USER-SERVICE")
public interface UserFeignClient {

    // userService CS_CATEGORY 전체 목록 조회
    @GetMapping("/cs-categories/v1")
    ApiResponse<List<CsCategoryResponseDTO>> getCsCategories();

    // userId로 닉네임 조회 (게시글 작성 시 사용)
    @PostMapping("/user/v1/info/nickname")
    ApiResponse<UserInfoResponseDTO> getNicknameByUserId(@RequestBody UserInfoRequestDTO rDTO);
}