package com.algotalk.userservice.controller;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.userservice.dto.response.CsCategoryResponseDTO;
import com.algotalk.userservice.service.ICsCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/cs-categories/v1")
@RequiredArgsConstructor
public class CsCategoryController {

    private final ICsCategoryService csCategoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CsCategoryResponseDTO>>> getCsCategories() throws Exception {
        log.info("{}.getCsCategories Start!", this.getClass().getName());

        List<CsCategoryResponseDTO> rList = csCategoryService.getCsCategories();

        log.info("{}.getCsCategories End!", this.getClass().getName());

        return ResponseEntity.ok(ApiResponse.ok(rList));
    }
}
