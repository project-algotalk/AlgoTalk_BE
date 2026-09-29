
package com.algotalk.userservice.dto.request;

import com.algotalk.common.pagination.Pagination;
import com.algotalk.userservice.domain.enums.AdminApprovalStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AdminApplicationListRequestDTO(
        @Min(1)
        Integer page,

        @Min(1)
        @Max(50)
        Integer size,

        AdminApprovalStatus status
) {
    public Pagination toPagination() {
        return Pagination.of(page == null ? 1 : page, size == null ? 10 : size);
    }
}