package com.meobeo.truyen.security;

import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        User user = userRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user với username: " + username));

        if (!user.getIsActive()) {
            throw new UsernameNotFoundException("Tài khoản chưa được kích hoạt: " + username);
        }

        return new CustomUserDetails(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        log.debug("Loading user by ID: {}", userId);

        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user với ID: " + userId));

        if (!user.getIsActive()) {
            throw new UsernameNotFoundException("Tài khoản chưa được kích hoạt với ID: " + userId);
        }

        return new CustomUserDetails(user);
    }
}