package com.algotalk.userservice.repository;

import com.algotalk.userservice.dto.command.AdminAccountCommand;
import com.algotalk.userservice.dto.response.AdminDashboardSummaryResponseDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IAdminAccountMapper {

    // 관리자 가입 신청
    int insertAdminAccount(AdminAccountCommand pCommand) throws Exception;

    // 관리자 승인 상태 조회
    AdminAccountCommand getAdminAccount(AdminAccountCommand pCommand) throws Exception;

    // 관리자 가입 신청 목록
    List<AdminAccountCommand> getAdminAccountLists(
            AdminAccountCommand pCommand
    ) throws Exception;

    // 관리자 대시보드 요약
    AdminDashboardSummaryResponseDTO getDashboardSummary() throws Exception;

    // 관리자 계정 승인
    int approveAdmin(AdminAccountCommand pCommand) throws Exception;

    // 관리자 계정 반려
    int rejectAdmin(AdminAccountCommand pCommand) throws Exception;

    // 관리자 계정 역할 변경
    int updateAdminRole(AdminAccountCommand pCommand) throws Exception;
}