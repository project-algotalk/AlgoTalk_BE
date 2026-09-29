package com.algotalk.userservice.repository;

import com.algotalk.userservice.dto.command.AdminAccountCommand;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IAdminAccountMapper {
    // 관리자 가입 신청
    int insertAdminAccount(AdminAccountCommand pCommand) throws Exception;
    // 관리자 승인상태 조회
    AdminAccountCommand getAdminAccount(AdminAccountCommand pCommand) throws Exception;
    // 관리자 가입신청 목록
    List<AdminAccountCommand> getAdminAccountLists(AdminAccountCommand pCommand) throws Exception;
    // 관리자 계정 승인
    int approveAdmin(AdminAccountCommand pCommand) throws Exception;
    // 관리자 계정 반려
    int rejectAdmin(AdminAccountCommand pCommand) throws Exception;
    // 관리자 계정 상태 변경
    int updateAdminRole(AdminAccountCommand pCommand) throws Exception;
}
