package com.algotalk.userservice.repository;

import com.algotalk.userservice.dto.command.SocialAccountCommand;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.UpdateEmailRequestDTO;
import com.algotalk.userservice.dto.request.UpdateNicknameRequestDTO;
import com.algotalk.userservice.dto.response.ExistsResponseDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IUserUpdateMapper {
    UserInfoCommand getMyPageSummaryByUserId(Long userId) throws Exception;
    List<SocialAccountCommand> getMyPageSocialAccountsByUserId(Long userId) throws Exception;
    List<UserInfoCommand> getMyPageTargetJobsByUserId(Long userId) throws Exception;
    List<UserInfoCommand> getMyPageEmploymentsByUserId(Long userId) throws Exception;

    // 현재 비밀번호 조회
    UserInfoCommand getUserInfoByUserId(UserInfoCommand rCommand) throws Exception;

    // 로그인 정보 변경
    // 비밀번호 변경
    int updatePassword(UserInfoCommand rCommand) throws Exception;

    // 회원 정보 변경
    // 닉네임 중복 확인
    ExistsResponseDTO getNicknameExists(UpdateNicknameRequestDTO pDTO) throws Exception;

    // 닉네임 변경
    int updateNickname(UserInfoCommand rCommand) throws Exception;

    // 이름 변경(이름은 중복 확인 필요 X)
    int updateName(UserInfoCommand rCommand) throws Exception;

    // 이메일 중복 확인
    ExistsResponseDTO getEmailExists(UpdateEmailRequestDTO rDTO) throws Exception;

    // 이메일 변경
    int updateEmail(UserInfoCommand rCommand) throws Exception;

    // 주소 변경(주소는 중복 확인 필요 X)
    int updateAddr(UserInfoCommand rCommand) throws Exception;
}