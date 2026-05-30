package com.algotalk.communityservice.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostCommand {

    private Long postId;            // 수정/삭제 시 사용
    private Long categoryId;        // 작성 시 사용
    private Long userId;            // 작성자 userId
    private String nickname;        // 작성 시 사용 (userService Feign 조회)
    private String title;
    private String content;
    private Long csCategoryId;
    private List<String> hashtags;
}