package com.algotalk.userservice.service;

import com.algotalk.userservice.dto.request.AdminApplicationListRequestDTO;
import com.algotalk.userservice.dto.request.AdminSignUpRequestDTO;
import com.algotalk.userservice.dto.response.AdminApplicationResponseDTO;
import com.algotalk.userservice.dto.response.AdminSignUpResponseDTO;
import com.algotalk.userservice.dto.response.AdminStatusResponseDTO;

import java.util.List;

public interface IAdminAccountService {
    // 관리자 가입 신청
    AdminSignUpResponseDTO signUp(AdminSignUpRequestDTO request) throws Exception;

    // 관리자 승인 상태 조회
    AdminStatusResponseDTO getStatus(Long userId) throws Exception;

    // 관리자 가입 신청 목록 조회
    List<AdminApplicationResponseDTO> getApplications(AdminApplicationListRequestDTO request) throws Exception;

    // 관리자 계정 승인
    void approve(Long targetUserId, Long approverUserId) throws Exception;

    // 관리자 계정 반려
    void reject(Long targetUserId, Long approverUserId, String reason) throws Exception;
}
