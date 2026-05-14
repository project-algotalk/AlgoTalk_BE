package com.algotalk.userservice.service;

import com.algotalk.userservice.dto.request.*;
import com.algotalk.userservice.dto.response.MyPageResponseDTO;


public interface IUserUpdateService {
    // 마이페이지
    MyPageResponseDTO getMyPage(Long userId) throws Exception;

    // 로그인 정보 변경
    // 아이디 변경
    int updateLoginId(Long userId, UpdateLoginIdRequestDTO pDTO) throws Exception;

    // 비밀번호 변경
    int updatePassword(Long userId, UpdatePasswordRequestDTO pDTO) throws Exception;
    // 소셜 가입자의 비밀번호 등록
    int setPassword(Long userId, SetPasswordRequestDTO pDTO) throws Exception;

    // 회원 정보 변경
    // 닉네임 변경
    int updateNickname(Long userId, UpdateNicknameRequestDTO pDTO) throws Exception;

    // 이름 변경
    int updateName(Long userId, UpdateNameRequestDTO pDTO) throws Exception;

    // 주소 변경
    int updateAddr(Long userId, UpdateAddrRequestDTO pDTO) throws Exception;

    // 이메일 중복 확인
    void isEmailDuplicated(UpdateEmailRequestDTO pDTO) throws Exception;

    // 이메일 변경
    int updateEmail(Long userId, UpdateEmailRequestDTO pDTO) throws Exception;
}
