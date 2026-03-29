package com.algotalk.userservice.repository;

import com.algotalk.userservice.dto.command.UserInfoCommand;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IUserRegMapper {

    // 회원가입 전 아이디 중복 확인
    UserInfoCommand getLoginIdExists(UserInfoCommand pCommand) throws Exception;

    // 회원가입 전 닉네임 중복 확인
    UserInfoCommand getNicknameExists(UserInfoCommand pCommand) throws Exception;

    // 회원가입 전 이메일 중복 확인
    UserInfoCommand getEmailExists(UserInfoCommand pCommand) throws Exception;

    // 회원 가입 - USER 테이블
    int insertUser(UserInfoCommand pCommand) throws Exception;

    // 회원 가입 - USER_CREDENTIAL 테이블
    int insertUserCredential(UserInfoCommand pCommand) throws Exception;

    // 회원 가입 - USER_ROLES 테이블
    int insertUserRoles(UserInfoCommand pCommand) throws Exception;

    // 회원가입 - USER_TARGET_JOB 테이블
    int insertUserTargetJob(UserInfoCommand pCommand);

    // 회원가입 - USER_EMPLOYMENT 테이블
    int insertUserEmployment(UserInfoCommand pCommand);
}