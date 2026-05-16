package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.command.SocialAccountCommand;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.*;
import com.algotalk.userservice.dto.response.ExistsResponseDTO;
import com.algotalk.userservice.dto.response.MyPageResponseDTO;
import com.algotalk.userservice.repository.IUserUpdateMapper;
import com.algotalk.userservice.service.IEmailService;
import com.algotalk.userservice.service.IUserUpdateService;
import com.algotalk.userservice.util.CmmUtil;
import com.algotalk.userservice.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static com.algotalk.userservice.exception.UserErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserUpdateService implements IUserUpdateService {

    private final IUserUpdateMapper userUpdateMapper;
    private final PasswordEncoder passwordEncoder;
    private final IEmailService emailService;

    @Override
    public MyPageResponseDTO getMyPage(Long userId) throws Exception {
        log.info("{}.getMyPage Start!", this.getClass().getName());

        // 1. 기본 정보 조회
        UserInfoCommand user = userUpdateMapper.getMyPageSummaryByUserId(userId);
        if (user == null) {
            log.warn("사용자 정보가 존재하지 않습니다.");
            throw new BusinessException(USER_NOT_FOUND);
        }

        // 2. 소셜 연동 계정 목록 조회
        List<String> providers = userUpdateMapper.getMyPageSocialAccountsByUserId(userId).stream()
                .map(SocialAccountCommand::getProvider)
                .filter(provider -> provider != null && !provider.isBlank())
                .distinct()
                .toList();

        // 3. 목표 직무 목록 조회
        List<MyPageResponseDTO.TargetJobInfoDTO> targetJobs =
                userUpdateMapper.getMyPageTargetJobsByUserId(userId).stream()
                    .map(job -> MyPageResponseDTO.TargetJobInfoDTO.builder()
                            .categoryId(job.getCategoryId())
                            .categoryName(job.getCategoryName())
                            .startDate(job.getStartDate())
                            .endDate(job.getEndDate())
                            .build())
                    .toList();

        // 4. 재직 이력 목록 조회
        List<MyPageResponseDTO.EmploymentInfoDTO> employments =
                userUpdateMapper.getMyPageEmploymentsByUserId(userId).stream()
                    .map(emp -> MyPageResponseDTO.EmploymentInfoDTO.builder()
                            .companyName(emp.getCompanyName())
                            .categoryId(emp.getEmploymentCategoryId())
                            .categoryName(emp.getEmploymentCategoryName())
                            .startDate(emp.getStartDate())
                            .endDate(emp.getEndDate())
                            .build())
                    .toList();

        // 5. 응답 DTO 조합
        MyPageResponseDTO rDTO = MyPageResponseDTO.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .name(user.getName())
                .email(EncryptUtil.decAES128CBC(CmmUtil.nvl(user.getEmail())))
                .addr1(user.getAddr1())
                .addr2(user.getAddr2())
                .createdAt(user.getCreatedAt())
                .loginId(user.getLoginId())
                .passwordSetYn(CmmUtil.nvl(user.getPasswordSetYn(), "Y"))
                .socialProviders(providers)
                .targetJobs(targetJobs)
                .employments(employments)
                .build();

        log.info("{}.getMyPage End!", this.getClass().getName());

        return rDTO;
    }

    @Transactional
    @Override
    public int updateLoginId(Long userId, UpdateLoginIdRequestDTO pDTO) throws Exception {
        log.info("{}.updateLoginId Start!", this.getClass().getName());

        // 1. 아이디 중복 확인
        UserInfoCommand rCommand = UserInfoCommand.builder()
                                    .userId(userId)
                                    .loginId(CmmUtil.nvl(pDTO.loginId()))
                                    .build();
        ExistsResponseDTO rDTO = userUpdateMapper.getLoginIdExists(rCommand);

        if ("Y".equals(rDTO.existsYn())) {
            throw new BusinessException(DUPLICATE_LOGIN_ID);
        }

        // 2. 아이디 변경
        int res = userUpdateMapper.updateLoginId(rCommand);

        if (res != 1) {
            throw new BusinessException(LOGIN_ID_UPDATE_FAIL);
        }

        log.info("{}.updateLoginId End!", this.getClass().getName());
        return res;
    }

    @Transactional
    @Override
    public int updatePassword(Long userId, UpdatePasswordRequestDTO pDTO) throws Exception {
        log.info("{}.updatePassword Start!", this.getClass().getName());

        log.info("userId: {}", userId);
        int res = 0;

        // 1. 변경할 비밀번호가 일치하는지 검증
        boolean passwordConfirmed = pDTO.isPasswordConfirmed();
        if(!passwordConfirmed) {
            log.warn("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
            throw new BusinessException(PASSWORD_MISMATCH);
        }

        UserInfoCommand pCommand = UserInfoCommand.builder()
                .userId(userId)
                .build();

        // 2. 현재 비밀번호 조회
        UserInfoCommand rCommand = userUpdateMapper.getUserInfoByUserId(pCommand);
        if(rCommand == null) {
            log.warn("사용자 정보가 존재하지 않습니다.");
            throw new BusinessException(USER_NOT_FOUND);
        }

        // 3. 현재 비밀번호가 입력한 현재 비밀번호와 일치하는지
        String passwordSetYn = CmmUtil.nvl(rCommand.getPasswordSetYn(), "Y");
        if ("Y".equals(passwordSetYn)) {
            boolean currentPasswordMatches = passwordEncoder.matches(pDTO.currentPassword(), rCommand.getPassword());
            if (!currentPasswordMatches) {
                log.warn("현재 비밀번호가 일치하지 않습니다.");
                throw new BusinessException(CUR_PASSWORD_MISMATCH);
            }
        }

        // 4. 현재 비밀번호와 동일한 비밀번호로 변경하지 못하도록 검증
        if(passwordEncoder.matches(pDTO.newPassword(), rCommand.getPassword())) {
            log.warn("현재 비밀번호와 동일한 비밀번호로 변경할 수 없습니다.");
            throw new BusinessException(NOW_PASSWORD_SAME);
        }

        // 5. 비밀번호 변경
        int updateCount = userUpdateMapper.updatePassword(
                UserInfoCommand.builder()
                    .userId(userId)
                    .password(passwordEncoder.encode(pDTO.newPassword()))
                    .build()
        );

        if (updateCount != 1) {
            log.error("비밀번호 변경이 실패했습니다.");
            throw new BusinessException(PASSWORD_UPDATE_FAIL);
        }

        res = 1; // 성공적으로 변경된 경우 1 반환

        log.info("{}.updatePassword End!", this.getClass().getName());
        return res;
    }

    @Transactional
    @Override
    public int setPassword(Long userId, SetPasswordRequestDTO pDTO) throws Exception {
        log.info("{}.setPassword Start!", this.getClass().getName());

        int res = 0;

        UserInfoCommand rDTO = userUpdateMapper.getUserInfoByUserId(UserInfoCommand.builder().userId(userId).build());

        // 1. 사용자 정보 존재하는지 확인
        if (rDTO == null) {
            log.warn("사용자 정보가 존재하지 않습니다.");
            throw new BusinessException(USER_NOT_FOUND);
        }

        // 2. 이미 비밀번호가 설정된 사용자는 현재 비밀번호를 확인해야 되기 때문에 setPassword 사용 불가
        // PASSWORD_SET_YN = 'Y'인 경우에만 체크
        if (!"N".equals(CmmUtil.nvl(rDTO.getPasswordSetYn(), "Y"))) {
            log.warn("이미 비밀번호가 설정된 사용자입니다. userID: {}", userId);
            throw new BusinessException(PASSWORD_ALREADY_SET);
        }

        // 3. 변경할 비밀번호가 일치하는지 검증
        if (!pDTO.isPasswordConfirmed()) {
            log.warn("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
            throw new BusinessException(PASSWORD_MISMATCH);
        }

        // 4. 비밀번호 변경
        res = userUpdateMapper.updatePassword(
                            UserInfoCommand.builder()
                                    .userId(userId)
                                    .password(passwordEncoder.encode(pDTO.newPassword()))
                                    .build()
                            );

        if (res != 1) {
            log.error("비밀번호 변경이 실패했습니다.");
            throw new BusinessException(PASSWORD_UPDATE_FAIL);
        }

        log.info("{}.updatePassword End!", this.getClass().getName());
        return res;
    }

    @Transactional
    @Override
    public int updateNickname(Long userId, UpdateNicknameRequestDTO pDTO) throws Exception {
        log.info("{}.updateNickname Start!", this.getClass().getName());

        log.info("userId: {}", userId);
        int res = 0;

        // 1. 닉네임 중복 검증
        UserInfoCommand pCommand = UserInfoCommand.builder()
                .userId(userId)
                .nickname(CmmUtil.nvl(pDTO.nickname().strip()))
                .build();

        ExistsResponseDTO rDTO = userUpdateMapper.getNicknameExists(pCommand);
        String existsYn = rDTO.existsYn();

        if(existsYn.equals("Y")) {
            log.warn("이미 사용 중인 닉네임입니다.");
            throw new BusinessException(DUPLICATE_NICKNAME);
        }

        // 2. 닉네임 변경
        res = userUpdateMapper.updateNickname(
                UserInfoCommand.builder()
                        .userId(userId)
                        .nickname(pDTO.nickname().strip())
                        .build());

        if(res != 1) {
            log.error("닉네임 변경이 실패했습니다.");
            throw new BusinessException(NICKNAME_UPDATE_FAIL);
        }

        log.info("{}.updateNickname End!", this.getClass().getName());
        return res;
    }

    @Transactional
    @Override
    public int updateName(Long userId, UpdateNameRequestDTO pDTO) throws Exception {
        log.info("{}.updateName Start!", this.getClass().getName());

        log.info("userId: {}", userId);
        int res = 0;

        // 1. 이름 변경
        res = userUpdateMapper.updateName(
                UserInfoCommand.builder()
                        .userId(userId)
                        .name(CmmUtil.nvl(pDTO.name()))
                        .build());

        if(res != 1) {
            log.error("이름 변경이 실패했습니다.");
            throw new BusinessException(NAME_UPDATE_FAIL);
        }

        log.info("{}.updateName End!", this.getClass().getName());
        return res;
    }

    @Transactional
    @Override
    public int updateAddr(Long userId, UpdateAddrRequestDTO pDTO) throws Exception {
        log.info("{}.updateAddr Start!", this.getClass().getName());

        log.info("userId: {}", userId);

        // 1. 주소 변경
        int res = userUpdateMapper.updateAddr(
                UserInfoCommand.builder()
                        .userId(userId)
                        .addr1(CmmUtil.nvl(pDTO.addr1()))
                        .addr2(CmmUtil.nvl(pDTO.addr2()))
                        .build());

        if(res != 1) {
            log.error("주소 변경이 실패했습니다.");
            throw new BusinessException(ADDR_UPDATE_FAIL);
        }

        log.info("{}.updateAddr End!", this.getClass().getName());
        return res;
    }

    @Override
    public void isEmailDuplicated(UpdateEmailRequestDTO pDTO) throws Exception {
        log.info("{}.isEmailDuplicated Start!", this.getClass().getName());

        // 1. 이메일 암호화
        UserInfoCommand pCommand = UserInfoCommand.builder()
                .email(EncryptUtil.encAES128CBC(pDTO.email().strip()))
                .build();

        // 2. DB 조회
        ExistsResponseDTO rDTO = userUpdateMapper.getEmailExists(pCommand);
        String existsYn = rDTO.existsYn();

        if(existsYn.equals("Y")) {
            log.warn("이미 사용 중인 이메일입니다.");
            throw new BusinessException(DUPLICATE_EMAIL);
        }

        log.info("{}.isEmailDuplicated End!", this.getClass().getName());
    }

    @Transactional
    @Override
    public int updateEmail(Long userId, UpdateEmailRequestDTO pDTO) throws Exception {
        log.info("{}.updateEmail Start!", this.getClass().getName());

        log.info("userId: {}", userId);

        // 1. 이메일 인증 여부 확인
        if (!emailService.isEmailVerified(pDTO.email().strip())) {
            throw new BusinessException(EMAIL_NOT_VERIFIED);
        }

        // 2. 이메일 변경
        int res = userUpdateMapper.updateEmail(
                UserInfoCommand.builder()
                        .userId(userId)
                        .email(EncryptUtil.encAES128CBC(pDTO.email().strip()))
                        .build()
        );

        if(res != 1) {
            log.error("이메일 변경이 실패했습니다.");
            throw new BusinessException(EMAIL_UPDATE_FAIL);
        }

        log.info("{}.updateEmail End!", this.getClass().getName());
        return res;
    }

    @Transactional
    @Override
    public int updateTargetJobs(Long userId, List<TargetJobRequestDTO> pDTO) throws Exception {
        log.info("{}.updateTargetJobs Start!", this.getClass().getName());

        // 1. 최대 3개 직무까지만 등록 가능하도록 검증
        if (pDTO != null && pDTO.size() > 3) {
            throw new BusinessException(TARGET_JOB_LIMIT_EXCEEDED);
        }

        // 2. 기존 목표 직무 삭제 (빈 리스트 요청 시 전체 삭제 후 종료)
        userUpdateMapper.deleteTargetJobsByUserId(UserInfoCommand.builder()
                .userId(userId)
                .build());
        if (pDTO == null || pDTO.isEmpty()) {
            return 1;
        }

        // 3. 새로운 목표 직무 등록
        for (TargetJobRequestDTO job : pDTO) {
            int res = userUpdateMapper.insertTargetJobByUserId(
                UserInfoCommand.builder()
                    .userId(userId)
                    .categoryId(job.categoryId())
                    .startDate(job.startDate())
                    .endDate(job.endDate() == null ? LocalDate.of(9999, 12, 31) : job.endDate())
                    .build());
            if (res != 1) {
                throw new BusinessException(TARGET_JOB_UPDATE_FAIL);
            }
        }

        log.info("{}.updateTargetJobs End!", this.getClass().getName());
        return 1;
    }

    @Transactional
    @Override
    public int updateEmployments(Long userId, List<EmploymentRequestDTO> pDTO) throws Exception {
        log.info("{}.updateEmployments Start!", this.getClass().getName());

        // 1. 기존 재직 이력 삭제 (빈 리스트 요청 시 전체 삭제 후 종료)
        userUpdateMapper.deleteEmploymentsByUserId(UserInfoCommand.builder()
                .userId(userId)
                .build());
        if (pDTO == null || pDTO.isEmpty()) {
            return 1;
        }

        // 2. 새로운 재직 이력 등록
        for (EmploymentRequestDTO emp : pDTO) {
            int res = userUpdateMapper.insertEmploymentByUserId(
                UserInfoCommand.builder()
                    .userId(userId)
                    .companyName(emp.companyName())
                    .categoryId(emp.categoryId())
                    .startDate(emp.startDate())
                    .endDate(emp.endDate() == null ? LocalDate.of(9999, 12, 31) : emp.endDate())
                    .build());
            if (res != 1) {
                throw new BusinessException(EMPLOYMENT_UPDATE_FAIL);
            }
        }

        log.info("{}.updateEmployments End!", this.getClass().getName());
        return 1;
    }
}