package com.algotalk.communityservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.communityservice.client.AiFeignClient;
import com.algotalk.communityservice.dto.command.HashTagCommand;
import com.algotalk.communityservice.dto.command.LikeScrapCommand;
import com.algotalk.communityservice.dto.command.PostCommand;
import com.algotalk.communityservice.dto.command.PostListCommand;
import com.algotalk.communityservice.dto.request.CsValidationRequestDTO;
import com.algotalk.communityservice.dto.response.CsCategoryResponseDTO;
import com.algotalk.communityservice.dto.response.CsValidationResponseDTO;
import com.algotalk.communityservice.dto.response.PostDetailResponseDTO;
import com.algotalk.communityservice.dto.response.PostListResponseDTO;
import com.algotalk.communityservice.dto.row.PostDetailRowDTO;
import com.algotalk.communityservice.dto.row.PostListRowDTO;
import com.algotalk.communityservice.exception.CommunityErrorCode;
import com.algotalk.communityservice.persistance.IRedisMapper;
import com.algotalk.communityservice.repository.ICommunityHashTagMapper;
import com.algotalk.communityservice.repository.ICommunityLikeScrapMapper;
import com.algotalk.communityservice.repository.ICommunityPostMapper;
import com.algotalk.communityservice.service.ICommunityPostService;
import com.algotalk.communityservice.service.ICsCategoryFeignService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.algotalk.communityservice.exception.CommunityErrorCode.POST_NOT_FOUND;
import static com.algotalk.communityservice.exception.CommunityErrorCode.POST_UNAUTHORIZED;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityPostService implements ICommunityPostService {

    private final ICommunityPostMapper communityPostMapper;
    private final ICommunityHashTagMapper communityHashTagMapper;
    private final ICommunityLikeScrapMapper likeScrapMapper;
    private final ICsCategoryFeignService csCategoryFeignService;
    private final IRedisMapper redisMapper;
    private final AiFeignClient aiFeignClient;

    @Override
    public List<PostListResponseDTO> getPostList(PostListCommand pCommand) {
        log.info("{}.getPostList Start!", this.getClass().getName());

        List<PostListRowDTO> rows = communityPostMapper.getPostList(pCommand);
        if (rows.isEmpty()) {
            log.info("{}.getPostList End!", this.getClass().getName());
            return List.of();
        }

        List<Long> postIds = rows.stream().map(PostListRowDTO::getPostId).toList();

        // Redis Pipeline으로 한 번에 조회
        Map<Long, Long> viewCountMap  = redisMapper.getViewCounts(postIds);
        Map<Long, Long> likeCountMap  = redisMapper.getLikeCounts(postIds);
        Map<Long, Long> scrapCountMap = redisMapper.getScrapCounts(postIds);

        // Redis miss → DB fallback + Redis set
        postIds.forEach(postId -> {
            if (!viewCountMap.containsKey(postId)) {
                Long dbCount = communityPostMapper.getViewCount(postId);
                long count = dbCount != null ? dbCount : 0L;
                viewCountMap.put(postId, count);
                redisMapper.setViewCount(postId, count);
            }
            if (!likeCountMap.containsKey(postId)) {
                Long dbCount = likeScrapMapper.getLikeCountFromDB(
                        LikeScrapCommand.builder().postId(postId).build()
                );
                long count = dbCount != null ? dbCount : 0L;
                likeCountMap.put(postId, count);
                redisMapper.setLikeCount(postId, count);
            }
            if (!scrapCountMap.containsKey(postId)) {
                Long dbCount = likeScrapMapper.getScrapCountFromDB(
                        LikeScrapCommand.builder().postId(postId).build()
                );
                long count = dbCount != null ? dbCount : 0L;
                scrapCountMap.put(postId, count);
                redisMapper.setScrapCount(postId, count);
            }
        });

        List<CsCategoryResponseDTO> csCategories = csCategoryFeignService.getCategories();
        Map<Long, CsCategoryResponseDTO> csCategoryMap = csCategories.stream()
                .collect(java.util.stream.Collectors.toMap(
                        CsCategoryResponseDTO::categoryId,
                        c -> c,
                        (a, b) -> a
                ));

        List<PostListResponseDTO> rList = rows.stream()
                .map(row -> {
                    Long postId = row.getPostId();
                    List<String> hashtags = communityHashTagMapper.getPostHashTags(
                            HashTagCommand.builder().postId(postId).build()
                    ).stream().map(HashTagCommand::getTagName).toList();

                    CsCategoryResponseDTO csCategory = csCategoryMap.get(row.getCsCategoryId());

                    return PostListResponseDTO.builder()
                            .postId(postId)
                            .categoryId(row.getCategoryId())
                            .categoryCd(row.getCategoryCd())
                            .categoryName(row.getCategoryName())
                            .userId(row.getUserId())
                            .nickname(row.getNickname())
                            .title(row.getTitle())
                            .contentPreview(row.getContent())
                            .isNotice(row.getIsNotice())
                            .viewCount(viewCountMap.getOrDefault(postId, (long) row.getViewCount()).intValue())
                            .likeCount(likeCountMap.getOrDefault(postId, (long) row.getLikeCount()).intValue())
                            .scrapCount(scrapCountMap.getOrDefault(postId, (long) row.getScrapCount()).intValue())
                            .csCategoryId(row.getCsCategoryId())
                            .csCategoryName(csCategory != null ? csCategory.categoryName() : null)
                            .csCategoryType(csCategory != null ? csCategory.categoryType() : null)
                            .hashtags(hashtags)
                            .createdAt(row.getCreatedAt())
                            .totalCount(row.getTotalCount())
                            .build();
                })
                .toList();

        log.info("{}.getPostList End!", this.getClass().getName());
        return rList;
    }

    @Override
    public PostDetailResponseDTO getPostDetail(PostCommand pCommand) {
        log.info("{}.getPostDetail Start!", this.getClass().getName());

        // 조회수 증가
        redisMapper.incrementViewCount(pCommand.getPostId());

        PostDetailRowDTO row = communityPostMapper.getPostDetail(pCommand);

        if (row == null) {
            throw new BusinessException(POST_NOT_FOUND);
        }

        // 해시태그 조회
        List<String> hashTagNames = getHashTagNames(pCommand.getPostId());

        // CS 카테고리 캐시에서 조회
        String csCategoryName = null;
        String csCategoryType = null;
        if (row.getCsCategoryId() != null) {
            CsCategoryResponseDTO csCategory = getCsCategory(row.getCsCategoryId());
            if (csCategory != null) {
                csCategoryName = csCategory.categoryName();
                csCategoryType = csCategory.categoryType();
            }
        }

        // Redis에서 실시간 카운트 조회
        Long likeCount = redisMapper.getLikeCount(pCommand.getPostId());
        Long scrapCount = redisMapper.getScrapCount(pCommand.getPostId());
        Long viewCount = redisMapper.getViewCount(pCommand.getPostId());

        // liked/scrapped 가능 여부 (비로그인 시 userId null -> false)
        Boolean liked = pCommand.getUserId() != null
                ? redisMapper.isUserLiked(pCommand.getPostId(), pCommand.getUserId())
                : false;
        Boolean scrapped = pCommand.getUserId() != null
                ? redisMapper.isUserScrapped(pCommand.getPostId(), pCommand.getUserId())
                : false;

        PostDetailResponseDTO rDTO = PostDetailResponseDTO.builder()
                .postId(row.getPostId())
                .categoryId(row.getCategoryId())
                .categoryCd(row.getCategoryCd())
                .categoryName(row.getCategoryName())
                .userId(row.getUserId())
                .nickname(row.getNickname())
                .title(row.getTitle())
                .content(row.getContent())
                .isNotice(row.getIsNotice())
                .viewCount(viewCount != null ? viewCount.intValue() : row.getViewCount())
                .likeCount(likeCount != null ? likeCount.intValue() : row.getLikeCount())
                .scrapCount(scrapCount != null ? scrapCount.intValue() : row.getScrapCount())
                .commentCount(row.getCommentCount())
                .liked(liked)
                .scrapped(scrapped)
                .isScrapable(row.getIsScrapable())
                .createdAt(row.getCreatedAt())
                .updatedAt(row.getUpdatedAt())
                .csCategoryId(row.getCsCategoryId())
                .csCategoryName(csCategoryName)
                .csCategoryType(csCategoryType)
                .hashtags(hashTagNames)
                .build();

        log.info("{}.getPostDetail End!", this.getClass().getName());
        return rDTO;
    }

    @Override
    @Transactional
    public Long insertPost(PostCommand pCommand) {
        log.info("{}.insertPost Start!", this.getClass().getName());

        // 질문공유(categoryId=1) 카테고리일 때만 CS 검증
        if (pCommand.getCategoryId() != null && pCommand.getCategoryId() == 1L) {

            CsValidationResponseDTO validation;
            try {
                validation = aiFeignClient.validateCsQuestions(
                        CsValidationRequestDTO.builder()
                                .questions(List.of(pCommand.getTitle()))
                                .build()
                );
            } catch (FeignException.GatewayTimeout e) {
                log.error("[AI_CALL_FAILED][validateCsQuestions] 타임아웃. title={}", pCommand.getTitle(), e);
                throw new BusinessException(CommunityErrorCode.AI_CALL_FAILED);
            } catch (FeignException e) {
                log.error("[AI_CALL_FAILED][validateCsQuestions] HTTP 오류. status={}, title={}", e.status(), pCommand.getTitle(), e);
                throw new BusinessException(CommunityErrorCode.AI_CALL_FAILED);
            } catch (Exception e) {
                log.error("[AI_CALL_FAILED][validateCsQuestions] type={}, title={}, message={}",
                        e.getClass().getSimpleName(), pCommand.getTitle(), e.getMessage(), e);
                throw new BusinessException(CommunityErrorCode.AI_CALL_FAILED);
            }

            // 응답 검증
            if (validation == null || validation.results() == null || validation.results().isEmpty()) {
                log.error("[AI_CALL_FAILED][validateCsQuestions] 응답 payload 이상. title={}", pCommand.getTitle());
                throw new BusinessException(CommunityErrorCode.AI_CALL_FAILED);
            }

            boolean isValid = validation.results().get(0).isValid();
            if (!isValid) {
                log.warn("[NOT_CS_QUESTION] CS 관련 질문 아님. title={}", pCommand.getTitle());
                throw new BusinessException(CommunityErrorCode.NOT_CS_QUESTION);
            }
        }

        communityPostMapper.insertPost(pCommand);
        Long postId = pCommand.getPostId();
        saveHashTags(postId, pCommand.getHashtags(), pCommand.getUserId());

        log.info("{}.insertPost End!", this.getClass().getName());
        return postId;
    }

    @Override
    @Transactional
    public void updatePost(PostCommand pCommand) {
        log.info("{}.updatePost Start!", this.getClass().getName());

        // 게시글 존재 + 작성자 확인
        PostDetailRowDTO existing = communityPostMapper.getPostDetail(pCommand);
        if (existing == null) {
            throw new BusinessException(POST_NOT_FOUND);
        }
        if (!existing.getUserId().equals(pCommand.getUserId())) {
            throw new BusinessException(POST_UNAUTHORIZED);
        }

        // 질문공유(categoryId=1) 카테고리일 때만 CS 검증
        if (pCommand.getCategoryId() != null && pCommand.getCategoryId() == 1L) {
            CsValidationResponseDTO validation;
            try {
                validation = aiFeignClient.validateCsQuestions(
                        CsValidationRequestDTO.builder()
                                .questions(List.of(pCommand.getTitle()))
                                .build()
                );
            } catch (FeignException.GatewayTimeout e) {
                log.error("[AI_CALL_FAILED][validateCsQuestions] 타임아웃. title={}", pCommand.getTitle(), e);
                throw new BusinessException(CommunityErrorCode.AI_CALL_FAILED);
            } catch (FeignException e) {
                log.error("[AI_CALL_FAILED][validateCsQuestions] HTTP 오류. status={}, title={}", e.status(), pCommand.getTitle(), e);
                throw new BusinessException(CommunityErrorCode.AI_CALL_FAILED);
            } catch (Exception e) {
                log.error("[AI_CALL_FAILED][validateCsQuestions] type={}, title={}, message={}",
                        e.getClass().getSimpleName(), pCommand.getTitle(), e.getMessage(), e);
                throw new BusinessException(CommunityErrorCode.AI_CALL_FAILED);
            }

            if (validation == null || validation.results() == null || validation.results().isEmpty()) {
                log.error("[AI_CALL_FAILED][validateCsQuestions] 응답 payload 이상. title={}", pCommand.getTitle());
                throw new BusinessException(CommunityErrorCode.AI_CALL_FAILED);
            }

            boolean isValid = validation.results().get(0).isValid();
            if (!isValid) {
                log.warn("[NOT_CS_QUESTION] CS 관련 질문 아님. title={}", pCommand.getTitle());
                throw new BusinessException(CommunityErrorCode.NOT_CS_QUESTION);
            }
        }

        communityPostMapper.updatePost(pCommand);
        removeHashTags(pCommand.getPostId(), pCommand.getUserId());
        saveHashTags(pCommand.getPostId(), pCommand.getHashtags(), pCommand.getUserId());

        log.info("{}.updatePost End!", this.getClass().getName());
    }

    @Override
    @Transactional
    public void deletePost(PostCommand pCommand) {
        log.info("{}.deletePost Start!", this.getClass().getName());

        // 게시글 존재 + 작성자 확인
        PostDetailRowDTO existing = communityPostMapper.getPostDetail(pCommand);
        if (existing == null) {
            throw new BusinessException(POST_NOT_FOUND);
        }
        if (!existing.getUserId().equals(pCommand.getUserId())) {  // .userId() → .getUserId()
            throw new BusinessException(POST_UNAUTHORIZED);
        }

        communityPostMapper.deletePost(pCommand);
        removeHashTags(pCommand.getPostId(), pCommand.getUserId());

        log.info("{}.deletePost End!", this.getClass().getName());
    }

    // 해시태그 이름 목록 조회 헬퍼
    private List<String> getHashTagNames(Long postId) {
        return communityHashTagMapper.getPostHashTags(
                        HashTagCommand.builder().postId(postId).build()
                ).stream()
                .map(HashTagCommand::getTagName)
                .toList();
    }

    // CS 카테고리 캐시 조회 헬퍼
    private CsCategoryResponseDTO getCsCategory(Long csCategoryId) {
        return csCategoryFeignService.getCategories()
                .stream()
                .filter(c -> c.categoryId().equals(csCategoryId))
                .findFirst()
                .orElse(null);
    }

    // 해시태그 저장 헬퍼
    private void saveHashTags(Long postId, List<String> hashtags, Long userId) {
        if (hashtags == null || hashtags.isEmpty()) return;

        for (String tagName : hashtags) {
            HashTagCommand tagCommand = HashTagCommand.builder()
                    .tagName(tagName)
                    .userId(userId)
                    .build();

            HashTagCommand existing = communityHashTagMapper.getHashTagId(tagCommand);

            Long hashtagId;
            if (existing == null) {
                // 없으면 신규 등록
                communityHashTagMapper.insertHashTag(tagCommand);
                hashtagId = tagCommand.getHashtagId();
            } else {
                // 있으면 사용 횟수 증가
                hashtagId = existing.getHashtagId();
                communityHashTagMapper.incrementUseCount(
                        HashTagCommand.builder()
                                .hashtagId(hashtagId)
                                .userId(userId)
                                .build()
                );
            }

            // 게시글-해시태그 매핑 등록
            communityHashTagMapper.insertPostHashTag(
                    HashTagCommand.builder()
                            .postId(postId)
                            .hashtagId(hashtagId)
                            .userId(userId)
                            .build()
            );
        }
    }

    // 해시태그 수정/삭제 핼퍼
    private void removeHashTags(Long postId, Long userId) {
        // 1. 현재 게시글 해시태그 ID 목록 조회
        List<HashTagCommand> postHashTagIds = communityHashTagMapper.getPostHashTagIds(
                HashTagCommand.builder().postId(postId).build()
        );

        // 2. 매핑 삭제
        communityHashTagMapper.deletePostHashTags(
                HashTagCommand.builder().postId(postId).build()
        );

        // 3. USE_COUNT 감소 + 0이면 원장에서도 삭제
        for (HashTagCommand tag : postHashTagIds) {
            communityHashTagMapper.decrementUseCount(
                    HashTagCommand.builder()
                            .hashtagId(tag.getHashtagId())
                            .userId(userId)
                            .build()
            );
            communityHashTagMapper.deleteUnusedHashTag(
                    HashTagCommand.builder()
                            .hashtagId(tag.getHashtagId())
                            .build()
            );
        }
    }
}
