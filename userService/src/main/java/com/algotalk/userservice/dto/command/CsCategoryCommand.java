package com.algotalk.userservice.dto.command;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CsCategoryCommand {
    private Long categoryId; // 카테고리 ID
    private String categoryType; // 카테고리 유형 (예: "COMMON_CS", "JOB")
    private String categoryName; // 카테고리 이름
    private Long parentId; // 상위 카테고리 ID (null이면 최상위 카테고리)
    private Integer depth; // 카테고리 깊이 (1: 최상위, 2: 하위, 3: 하위의 하위)
    private Integer sortOrder; // 카테고리 정렬 순서
}