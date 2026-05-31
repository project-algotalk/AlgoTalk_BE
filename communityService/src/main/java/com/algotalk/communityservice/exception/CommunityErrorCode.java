package com.algotalk.communityservice.exception;

import com.algotalk.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommunityErrorCode implements ErrorCode {

    // 게시글
    POST_NOT_FOUND          ("COMMUNITY_001", "게시글을 찾을 수 없습니다.",                  HttpStatus.NOT_FOUND),
    POST_ALREADY_DELETED    ("COMMUNITY_002", "이미 삭제된 게시글입니다.",                   HttpStatus.BAD_REQUEST),
    POST_UNAUTHORIZED       ("COMMUNITY_003", "게시글 작성자만 수정/삭제할 수 있습니다.",      HttpStatus.FORBIDDEN),
    NOT_CS_QUESTION         ("COMMUNITY_004", "CS 기술면접 관련 질문이 아닙니다.",                    HttpStatus.BAD_REQUEST),
    AI_CALL_FAILED          ("COMMUNITY_005", "AI 서비스 호출에 실패했습니다.",               HttpStatus.BAD_GATEWAY),

            // 댓글
    COMMENT_NOT_FOUND       ("COMMUNITY_011", "댓글을 찾을 수 없습니다.",                    HttpStatus.NOT_FOUND),
    COMMENT_ALREADY_DELETED ("COMMUNITY_012", "이미 삭제된 댓글입니다.",                     HttpStatus.BAD_REQUEST),
    COMMENT_UNAUTHORIZED    ("COMMUNITY_013", "댓글 작성자만 수정/삭제할 수 있습니다.",        HttpStatus.FORBIDDEN),
    COMMENT_DEPTH_EXCEEDED  ("COMMUNITY_014", "댓글은 최대 3Depth까지만 작성할 수 있습니다.", HttpStatus.BAD_REQUEST),

    // 좋아요 / 스크랩
    SCRAP_NOT_ALLOWED       ("COMMUNITY_021", "스크랩은 질문공유 게시판에서만 가능합니다.",    HttpStatus.BAD_REQUEST),

    // CS 카테고리 (OpenFeign)
    INVALID_CATEGORY_ID     ("COMMUNITY_031", "존재하지 않거나 허용되지 않는 카테고리입니다.", HttpStatus.BAD_REQUEST),
    CS_CATEGORY_FETCH_FAILED("COMMUNITY_032", "카테고리 정보를 불러오는 데 실패했습니다. 잠시 후 다시 시도해주세요.", HttpStatus.BAD_GATEWAY),

    // 권한
    UNAUTHORIZED            ("COMMUNITY_900", "로그인이 필요합니다.",      HttpStatus.UNAUTHORIZED),
    FORBIDDEN               ("COMMUNITY_901", "접근 권한이 없습니다.",      HttpStatus.FORBIDDEN),

    // 서버
    INTERNAL_ERROR          ("COMMUNITY_999", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    ;

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}