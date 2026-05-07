package com.algotalk.userservice.service;

import com.algotalk.userservice.dto.request.UpdateAddrRequestDTO;
import com.algotalk.userservice.dto.request.UpdateNameRequestDTO;
import com.algotalk.userservice.dto.request.UpdateNicknameRequestDTO;
import com.algotalk.userservice.dto.request.UpdatePasswordRequestDTO;


public interface IUserUpdateService {
    // 로그인 정보 변경
    // 비밀번호 변경
    int updatePassword(Long userId, UpdatePasswordRequestDTO pDTO) throws Exception;

    // 회원 정보 변경
    // 닉네임 변경
    int updateNickname(Long userId, UpdateNicknameRequestDTO pDTO) throws Exception;

    // 이름 변경
    int updateName(Long userId, UpdateNameRequestDTO pDTO) throws Exception;

    // 주소 변경
    int updateAddr(Long userId, UpdateAddrRequestDTO pDTO) throws Exception;
}
