package com.algotalk.userservice.repository;

import com.algotalk.userservice.dto.command.UserInfoCommand;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IUserWithdrawMapper {
    // 회원 탈퇴
    int withdrawUser(UserInfoCommand rCommand) throws Exception;

    // 회원 탈퇴 시 로그인 ID 변경
    int withdrawUserCredential(UserInfoCommand rCommand) throws Exception;

    // 회원 소셜 연동 정보 삭제
    int deleteAllSocialAccountsByUserId(UserInfoCommand rCommand) throws Exception;

    // 회원 소셜 연동 수 조회
    int getSocialAccountCountByUserId(UserInfoCommand rCommand) throws Exception;

    // provider별 소셜 연동 해제
    int deleteSocialAccountByProvider(UserInfoCommand rCommand) throws Exception;
}
