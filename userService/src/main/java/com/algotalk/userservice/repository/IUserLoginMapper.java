package com.algotalk.userservice.repository;

import com.algotalk.userservice.dto.command.UserInfoCommand;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IUserLoginMapper {

    /**
     * loginId로 사용자 인증 정보 조회
     *
     * USERS + USER_CREDENTIAL + USER_ROLES 테이블에서 필요한 정보를 JOIN하여 조회
     * @param pCommand
     * @return
     * @throws Exception
     */
    UserInfoCommand getUserAuthInfo(UserInfoCommand pCommand) throws Exception;

}
