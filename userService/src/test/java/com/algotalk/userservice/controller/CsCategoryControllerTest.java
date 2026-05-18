package com.algotalk.userservice.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CsCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("CS 카테고리 조회 성공")
    void getCsCategories_success() throws Exception {
        mockMvc.perform(get("/cs-categories/v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}