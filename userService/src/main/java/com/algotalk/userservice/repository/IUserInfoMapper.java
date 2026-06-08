package com.algotalk.userservice.repository;

import com.algotalk.userservice.dto.command.UserInfoCommand;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IUserInfoMapper {
    // userId로 닉네임 조회 (communityService 내부 API용)
    UserInfoCommand getNicknameByUserId(UserInfoCommand pCommand);
}
