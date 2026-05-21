package com.algotalk.interviewservice.dto.response;

public record CsCategoryResponseDTO(
        Long categoryId,     // 카테고리 ID
        String categoryType, // 카테고리 유형 (COMMON_CS / JOB)
        String categoryName, // 카테고리 이름
        Long parentId,       // 상위 카테고리 ID (null이면 최상위)
        Integer depth,       // 카테고리 깊이 (1: 최상위, 2: 하위, 3: 하위의 하위)
        Integer sortOrder    // 카테고리 정렬 순서
) {}