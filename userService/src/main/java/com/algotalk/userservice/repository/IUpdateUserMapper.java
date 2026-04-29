package com.algotalk.userservice.repository;

import com.algotalk.userservice.dto.command.UserInfoCommand;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IUpdateUserMapper {
    // 현재 비밀번호 조회
    UserInfoCommand getUserInfoByUserId(UserInfoCommand rCommand) throws Exception;
    // 로그인 정보 변경
    // 비밀번호 변경
    int updatePassword(UserInfoCommand rCommand) throws Exception;
}