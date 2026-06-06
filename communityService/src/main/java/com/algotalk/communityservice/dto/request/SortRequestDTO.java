package com.algotalk.communityservice.dto.request;

import lombok.Builder;
import lombok.Getter;

import java.util.Locale;

@Builder
public record SortRequestDTO(
    String sortType
) {
    public String getSortType() {
        if (sortType == null || sortType.isBlank()) {
            return "ASC"; // 기본값
        }
        return sortType.strip().toUpperCase(Locale.ROOT);
    }
}
