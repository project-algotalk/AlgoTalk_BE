package com.algotalk.userservice.auth;

import com.algotalk.userservice.dto.auth.UserAuthDTO;
import com.algotalk.userservice.dto.response.UserInfoResponseDTO;
import com.algotalk.userservice.util.CmmUtil;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Spring Security에서 인증된 사용자 정보를 담는 UserDetails 구현체
 * @param userAuthDTO
 */
@Slf4j
public record CustomUserDetails(
        UserAuthDTO userAuthDTO
) implements UserDetails {

    /**
     * 사용자의 권한 정보를 반환하는 메서드
     * @return
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> pSet = new HashSet<>();

        List<String> roles = userAuthDTO.roles();
        if(!roles.isEmpty()) {
            for(String role : roles) {
                pSet.add(new SimpleGrantedAuthority(role));
            }
        }

        return pSet;
    }

    /**
     * 사용자의 패스워드를 반환하는 메서드
     * @return
     */
    @Override
    public @Nullable String getPassword() {
        return CmmUtil.nvl(userAuthDTO.password());
    }

    /**
     * 사용자의 UNIQUE한 식별자 ID를 반환하는 메서드
     * @return userId
     */
    @Override
    public String getUsername() {
        return CmmUtil.nvl(userAuthDTO.userId());
    }

    /**
     * 계정이 만료되지 않았는지 여부를 반환하는 메서드
     * @return
     */
    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    /**
     * 계정이 잠겨있지 않은지 여부를 반환하는 메서드
     * @return
     */
    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    /**
     * 패스워드가 만료되지 않았는지 여부를 반환하는 메서드
     * @return
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    /**
     * 계정이 활성화되어 있는지 여부를 반환하는 메서드
     * @return
     */
    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}
