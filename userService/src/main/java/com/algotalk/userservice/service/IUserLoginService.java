package com.algotalk.userservice.service;

import com.algotalk.userservice.dto.request.LoginRequestDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface IUserLoginService {
    /**
     * 사용자 로그인과 신규 로그인 세션 생성
     *
     * @param pDTO 로그인 ID와 비밀번호를 포함한 요청 정보
     * @param response Access Token과 Refresh Token 쿠키를 설정할 HTTP 응답
     * @throws Exception 사용자 인증 또는 토큰 발급 실패
     */
    void login(LoginRequestDTO pDTO, HttpServletResponse response) throws Exception;

    /**
     * 현재 로그인 세션 로그아웃
     *
     * 요청의 Refresh Token 쿠키에서 sessionId를 추출하여 해당 세션만 삭제
     *
     * @param userId 인증된 사용자 ID
     * @param request Refresh Token 쿠키를 포함한 HTTP 요청
     * @param response 인증 쿠키를 만료시킬 HTTP 응답
     * @throws Exception Refresh Token 해석 또는 세션 삭제 실패
     */
    void logout(Long userId, HttpServletRequest request, HttpServletResponse response) throws Exception;

    /**
     * 모든 기기 로그인 세션 로그아웃
     *
     * @param userId 전체 로그인 세션을 삭제할 사용자 ID
     * @param response 인증 쿠키를 만료시킬 HTTP 응답
     * @throws Exception 전체 Refresh Token 세션 삭제 실패
     */
    void logoutAll(Long userId, HttpServletResponse response) throws Exception;
}
