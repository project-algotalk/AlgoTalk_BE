package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.EmploymentRequestDTO;
import com.algotalk.userservice.dto.request.SignUpRequestDTO;
import com.algotalk.userservice.dto.request.TargetJobRequestDTO;
import com.algotalk.userservice.dto.response.SignUpResponseDTO;
import com.algotalk.userservice.exception.UserErrorCode;
import com.algotalk.userservice.repository.IUserRegMapper;
import com.algotalk.userservice.service.IEmailService;
import com.algotalk.userservice.service.IUserRegService;
import com.algotalk.userservice.util.CmmUtil;
import com.algotalk.userservice.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegService implements IUserRegService {

    private final IUserRegMapper userRegMapper;
    private final IEmailService emailService;

    @Override
    public boolean isLoginIdDuplicated(SignUpRequestDTO pDTO) throws Exception {
        log.info("{}.isLoginIdDuplicated Start!", this.getClass().getName());

        UserInfoCommand pCommand = UserInfoCommand.from(pDTO);

        // 1. 값 잘 넘어왔는지 확인(로그인 아이디)
        String loginId = CmmUtil.nvl(pCommand.getLoginId());
        log.info("loginId: {}", loginId);

        // 2. DB 조회
        UserInfoCommand rCommand = userRegMapper.getLoginIdExists(pCommand);
        String existsYn = rCommand.getExistsYn();
        log.info("existsYn from DB: {}", existsYn);

        // 3. DB 조회 결과에 따라 중복 여부 판단 후 반환
        boolean loginIdDuplicated = existsYn.equals("Y");
        log.info("loginIdDuplicated: {}", loginIdDuplicated);

        if(loginIdDuplicated) {
            throw new BusinessException(UserErrorCode.DUPLICATE_LOGIN_ID);
        }

        log.info("{}.isLoginIdDuplicated End!", this.getClass().getName());
        return loginIdDuplicated; // Y: 중복 / N: 중복 아님
    }

    @Override
    public boolean isNicknameDuplicated(SignUpRequestDTO pDTO) throws Exception {
        log.info("{}.isNicknameDuplicated Start!", this.getClass().getName());

        try {
            UserInfoCommand pCommand = UserInfoCommand.from(pDTO);

            // 1. 값 잘 넘어왔는지 확인(닉네임)
            String nickName = CmmUtil.nvl(pCommand.getNickname());
            log.info("nickName: {}", nickName);

            // 2. DB 조회
            UserInfoCommand rCommand = userRegMapper.getNicknameExists(pCommand);
            String existsYn = rCommand.getExistsYn();
            log.info("existsYn from DB: {}", existsYn);

            // 3. DB 조회 결과에 따라 중복 여부 판단 후 반환
            boolean nicknameDuplicated = existsYn.equals("Y");

            if(nicknameDuplicated) {
                throw new BusinessException(UserErrorCode.DUPLICATE_NICKNAME);
            }

            return nicknameDuplicated; // Y: 중복 / N: 중복 아님
        } catch(BusinessException e) {
            log.error("{}.isNicknameDuplicated BusinessException: {}", this.getClass().getName(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("회원 가입 중 예상치 못한 오류 발생: {}", e.getMessage());
            throw new BusinessException(UserErrorCode.SIGN_UP_FAIL);
        } finally {
            log.info("{}.isNicknameDuplicated End!", this.getClass().getName());
        }
    }

    @Override
    public boolean isEmailDuplicated(SignUpRequestDTO pDTO) throws Exception {
        log.info("{}.isEmailDuplicated Start!", this.getClass().getName());

        UserInfoCommand pCommand = UserInfoCommand.from(pDTO);

        // 1. 값 잘 넘어왔는지 확인(이메일)
        String email = CmmUtil.nvl(pCommand.getEmail());
        log.info("email: {}", email);

        // 2. DB 조회
        UserInfoCommand rCommand = userRegMapper.getEmailExists(pCommand);
        String existsYn = rCommand.getExistsYn();
        log.info("existsYn from DB: {}", existsYn);

        // 3. DB 조회 결과에 따라 중복 여부 판단 후 반환
        boolean emailDuplicated = existsYn.equals("Y");

        if(emailDuplicated) {
            throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
        }

        log.info("{}.isEmailDuplicated End!", this.getClass().getName());
        return emailDuplicated; // Y: 중복 / N: 중복 아님
    }

    @Transactional
    @Override
    public SignUpResponseDTO insertUser(SignUpRequestDTO pDTO) throws Exception {
        log.info("{}.insertUser Start!", this.getClass().getName());


        // 비즈니스 로직 try-catch문으로 감싸서 예외 발생 시 적절한 에러 코드와 메시지로 변환하여 던지기
        try {
            // 1. USERS
            // 1.1. 비밀번호 입력 & 비밀번호 다시 확인 일치여부 비교
            if(!pDTO.isPasswordConfirmed()) {
                // 비밀번호 불일치
                log.error("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
                throw new BusinessException(UserErrorCode.PASSWORD_MISMATCH);
            }

            // 1.2. 이메일 인증 여부 확인
            if (!emailService.isEmailVerified(pDTO.email())) {
                throw new BusinessException(UserErrorCode.EMAIL_NOT_VERIFIED);
            }


            // 1.3. 값 잘 넘어왔는지 확인하고, UserInfoCommand로 변환(비밀번호, 이메일 암호화)
            UserInfoCommand pCommand = UserInfoCommand.builder()
                    // USERS
                    .email(EncryptUtil.encAES128CBC(CmmUtil.nvl(pDTO.email())))
                    .nickname(CmmUtil.nvl(pDTO.resolvedNickname()))
                    .name(CmmUtil.nvl(pDTO.name()))
                    .addr1(CmmUtil.nvl(pDTO.addr1()))
                    .addr2(CmmUtil.nvl(pDTO.addr2()))
                    // USER_CREDENTIAL
                    .loginId(CmmUtil.nvl(pDTO.loginId()))
                    .password(EncryptUtil.encHashSHA256(CmmUtil.nvl(pDTO.password())))
                    .build();

            // 1.4. USER 테이블에 INSERT (userId 채번)
            userRegMapper.insertUser(pCommand);

            // 2. USER_CREDENTIAL
            // 2.1. USER_CREDENTIAL 테이블에 INSERT (userId, loginId, password)
            userRegMapper.insertUserCredential(pCommand);

            // 3. USER_ROLES
            // 3.1. USER_ROLES 테이블에 INSERT (userId, role)
            userRegMapper.insertUserRoles(
                    UserInfoCommand.builder()
                            .userId(pCommand.getUserId())
                            .role("ROLE_USER") // 기본 역할로 "ROLE_USER" 저장
                            .build()
            );

            // 4. USER_TARGET_JOB
            // 4.1. USER_TARGET_JOB 테이블에 INSERT (userId, categoryId, categoryName, startDate, endDate)
            List<TargetJobRequestDTO> targetJobs = pDTO.targetJobs();
            List<String> targetJobNames = new ArrayList<>();
            if(targetJobs != null && !targetJobs.isEmpty()) {
                for(TargetJobRequestDTO job : targetJobs) {
                    log.info("목표 직무 정보: categoryId={}, categoryName={}, startDate={}, endDate={}",
                            job.categoryId(), job.categoryName(), job.startDate(), job.endDate());

                    targetJobNames.add(job.categoryName());

                    userRegMapper.insertUserTargetJob(
                            UserInfoCommand.builder()
                                    .userId(pCommand.getUserId())
                                    .categoryId(job.categoryId())
                                    .categoryName(job.categoryName())
                                    .startDate(job.startDate())
                                    .endDate(job.endDate())
                                    .build()
                    );
                }
            }

            // 5. USER_EMPLOYMENT
            // 5.1. USER_EMPLOYMENT 테이블에 INSERT (userId, categoryId, categoryName, companyName, startDate, endDate)
            List<EmploymentRequestDTO> employments = pDTO.employments();
            if(employments != null && !employments.isEmpty()) {
                for(EmploymentRequestDTO emp : employments) {
                    log.info("재직 이력 정보: categoryId={}, categoryName={}, companyName={}, startDate={}, endDate={}",
                            emp.categoryId(), emp.categoryName(), emp.companyName(), emp.startDate(), emp.endDate());

                    userRegMapper.insertUserEmployment(
                            UserInfoCommand.builder()
                                    .userId(pCommand.getUserId())
                                    .categoryId(emp.categoryId())
                                    .categoryName(emp.categoryName())
                                    .companyName(emp.companyName())
                                    .startDate(emp.startDate())
                                    .endDate(emp.endDate())
                                    .build()
                    );
                }
            }

            SignUpResponseDTO rDTO = SignUpResponseDTO.builder()
                    .userId(pCommand.getUserId())
                    .email(EncryptUtil.decAES128CBC(pCommand.getEmail()))
                    .nickname(pCommand.getNickname())
                    .targetJobs(targetJobNames)
//                .createdAt(pCommand.getCreatedAt())
                    .build();

            return rDTO;

        } catch (BusinessException e) {
            log.error("{}.insertUser BusinessException: {}", this.getClass().getName(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("회원 가입 중 예상치 못한 오류 발생: {}", e.getMessage());
            throw new BusinessException(UserErrorCode.SIGN_UP_FAIL);
        } finally {
            log.info("{}.insertUser End!", this.getClass().getName());
        }
    }
}
