package com.algotalk.userservice.repository;

import com.algotalk.userservice.dto.command.UserInfoCommand;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IUserFindMapper {
    // 이름 + 이메일로 아이디 조회
    UserInfoCommand findLoginIdByNameAndEmail(UserInfoCommand pCommand) throws Exception;

    // 아이디 + 이름 + 이메일로 사용자 존재 여부 확인
    UserInfoCommand findUserByLoginIdAndNameAndEmail(UserInfoCommand pCommand) throws Exception;

    // 비밀번호 변경
    void updatePassword(UserInfoCommand pCommand) throws Exception;
}
