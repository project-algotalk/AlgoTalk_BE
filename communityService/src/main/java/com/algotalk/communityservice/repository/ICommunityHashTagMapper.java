package com.algotalk.communityservice.repository;

import com.algotalk.communityservice.dto.command.HashTagCommand;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ICommunityHashTagMapper {
    // 해시태그 조회 (tagName으로 hashtagId 반환)
    HashTagCommand getHashTagId(HashTagCommand pCommand);

    // 해시태그 등록
    int insertHashTag(HashTagCommand pCommand);

    // 해시태그 사용 횟수 증가
    int incrementUseCount(HashTagCommand pCommand);

    // 게시글-해시태그 매핑 등록
    int insertPostHashTag(HashTagCommand pCommand);

    // 게시글-해시태그 매핑 전체 삭제 (수정 시 재등록)
    int deletePostHashTags(HashTagCommand pCommand);

    // 게시글 해시태그 목록 조회
    List<HashTagCommand> getPostHashTags(HashTagCommand pCommand);
}
