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

    // userId와 provider로 소셜 계정 존재 여부 확인
    boolean existsByUserIdAndProvider(SocialAccountCommand pCommand) throws Exception;

    // provider와 providerId로 소셜 계정 존재 여부 확인
    boolean existsByProviderAndProviderId(SocialAccountCommand pCommand) throws Exception;
}
