package com.algotalk.userservice.service.impl;

import com.algotalk.userservice.auth.CustomUserDetails;
import com.algotalk.userservice.dto.auth.UserAuthDTO;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.repository.IUserLoginMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final IUserLoginMapper userLoginMapper;

    @SneakyThrows
    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        log.info("{}.loadUserByUsername Start! loginId={}", this.getClass().getSimpleName(), loginId);

        UserInfoCommand rCommand = userLoginMapper.getUserAuthInfo(
                UserInfoCommand.builder()
                        .loginId(loginId)
                        .build()
        );

        if (rCommand == null) {
            log.warn("사용자 정보를 찾을 수 없습니다: loginId={}", loginId);
            throw new UsernameNotFoundException(loginId);
        }

        log.info("{}.loadUserByUsername End!", this.getClass().getSimpleName());
        return new CustomUserDetails(new UserAuthDTO(
                rCommand.getUserId(),
                rCommand.getLoginId(),
                rCommand.getPassword(),
                List.of(rCommand.getRole())
        ));
    }
}