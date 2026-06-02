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

    // 게시글 해시태그 ID 목록 조회 (감소 처리용)
    List<HashTagCommand> getPostHashTagIds(HashTagCommand pCommand);

    // 해시태그 사용 횟수 감소
    int decrementUseCount(HashTagCommand pCommand);

    // USE_COUNT = 0인 해시태그 삭제
    int deleteUnusedHashTag(HashTagCommand pCommand);

    // 게시글 목록 해시태그 일괄 조회(N+1 문제 방지)
    List<HashTagCommand> getPostHashTagsByPostIds(List<Long> postIds);
}
