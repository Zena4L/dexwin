package com.clement.dexwin.domain.security;

import com.clement.dexwin.domain.models.User;
import com.clement.dexwin.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Optional<User> user = userRepository.findUserByEmailAndIsActive(username);

        return user.map(SecurityUser::new).orElseThrow(() -> new UsernameNotFoundException("user not found"));
    }
}