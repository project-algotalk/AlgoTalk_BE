package com.algotalk.common.pagination;

import lombok.Getter;

@Getter
public class Pagination {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;

    private final int page; // 페이지 번호 (1부터 시작)
    private final int size; // 페이지당 항목 수 (기본값: 10, 최대값: 50)
    private final int offset; // 데이터베이스 쿼리에서 사용할 오프셋 (0부터 시작) 오프셋은 해당 페이지의 첫 번째 항목이 전체 데이터에서 몇 번째 위치에 있는지를 나타냅니다. 예를 들어, 페이지가 1이고 사이즈가 10이면 오프셋은 0이 됩니다. 페이지가 2이고 사이즈가 10이면 오프셋은 10이 됩니다.

    private Pagination(int page, int size) {
        this.page = page;
        this.size = size;
        this.offset = (page - 1) * size;
    }

    public static Pagination of(int page, int size) {
        if (page <= 0) page = DEFAULT_PAGE;
        if (size <= 0) size = DEFAULT_SIZE;
        if (size > MAX_SIZE) size = MAX_SIZE;
        return new Pagination(page, size);
    }

    // 사이즈 직접 지정(대시보드는 기본 값이 다름)
    public static Pagination of(int page, int size, int defaultSize) {
        if (page <= 0) page = DEFAULT_PAGE;
        if (size <= 0) size = defaultSize;
        if (size > MAX_SIZE) size = MAX_SIZE;
        return new Pagination(page, size);
    }

    public static Pagination ofDefault() {
        return new Pagination(DEFAULT_PAGE, DEFAULT_SIZE);
    }
}