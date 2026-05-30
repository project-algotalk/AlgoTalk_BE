package com.algotalk.communityservice.dto.command;

import lombok.Builder;
import lombok.Getter;

@Builder
public class PostListCommand {

    private Long categoryId;        // 게시판 카테고리 ID 필터
    private String categoryCd;      // 카테고리 코드 필터 (QUESTION/INFO/REVIEW/FREE)
    private String keyword;         // 검색 키워드
    private String searchType;      // 검색 타입 (TITLE / CONTENT / TITLE_CONTENT / AUTHOR)
    private Long csCategoryId;      // CS 카테고리 태그 필터
    private String hashtag;         // 해시태그 필터
    private int page;
    private int size;
    private int offset;             // MyBatis 페이징용 (page-1)*size
}