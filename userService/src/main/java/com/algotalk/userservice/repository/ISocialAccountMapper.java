package com.algotalk.userservice.repository;

import com.algotalk.userservice.dto.command.SocialAccountCommand;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ISocialAccountMapper {

    /**
     * 소셜 제공자와 제공자 ID로 사용자 정보 조회
     * @param pCommand
     * @return
     * @throws Exception
     */
    UserInfoCommand findByProviderAndProviderId(SocialAccountCommand pCommand) throws Exception;

    /**
     * 소셜 계정 저장
     * @param pCommand
     * @throws Exception
     */
    void insertSocialAccount(SocialAccountCommand pCommand) throws Exception;

    // 소셜 연결 해제 - 물리 삭제
    int deleteSocialAccount(SocialAccountCommand pCommand) throws Exception;

    // 연결된 소셜 수 조회
    int countByUserId(Long userId) throws Exception;

    // userId + provider로 소셜 계정 존재 여부 조회
    SocialAccountCommand findByUserIdAndProvider(SocialAccountCommand pCommand) throws Exception;

    // 탈퇴 시 전체 소셜 계정 물리 삭제
    int deleteAllByUserId(Long userId) throws Exception;
}
