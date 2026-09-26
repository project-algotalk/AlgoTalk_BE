package com.algotalk.userservice.dto.response;

import java.util.List;

public record AdminUserPageResponseDTO(
        List<AdminUserResponseDTO> content,
        int page,
        int size,
        long totalCount,
        int totalPages
) {
}
