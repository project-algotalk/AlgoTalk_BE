package com.algotalk.userservice.service.impl;

import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.FindLoginIdRequestDTO;
import com.algotalk.userservice.repository.IUserFindMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FindAccountServiceMockTest {

    @InjectMocks
    private FindAccountService findAccountService;

    @Mock
    private EmailService emailService;

    @Mock
    private IUserFindMapper userFindMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StringRedisTemplate stringRedisTemplate;


    @Test
    @DisplayName("아이디 찾기 이메일 발송 성공 - EmailService 호출 검증")
    void sendFindLoginIdEmail_success() throws Exception {
        // given
        FindLoginIdRequestDTO pDTO = FindLoginIdRequestDTO.builder()
                .name("홍길동")
                .email("test@algotalk.com")
                .build();

        // given
        given(userFindMapper.findLoginIdByNameAndEmail(any()))
                .willReturn(
                        UserInfoCommand.builder()
                                .loginId("testId")
                                .build()
                );

        // when
        findAccountService.sendFindLoginIdEmail(pDTO);

        // then
        verify(emailService, times(1))
                .sendEmailVerificationCode(any());
    }
}