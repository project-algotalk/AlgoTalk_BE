package com.algotalk.common.pagination;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaginationRequestDTO {

    @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
    private Integer page;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 50, message = "페이지 크기는 최대 50까지 가능합니다.")
    private Integer size;

    public Pagination toPagination() {
        return Pagination.of(
                page != null ? page : 1,
                size != null ? size : 10
        );
    }
}