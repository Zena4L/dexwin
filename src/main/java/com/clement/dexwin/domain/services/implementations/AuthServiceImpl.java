package com.clement.dexwin.domain.services.implementations;

import com.clement.dexwin.domain.dtos.users.LoginRequestDto;
import com.clement.dexwin.domain.dtos.users.SignedUpSucessResponse;
import com.clement.dexwin.domain.dtos.users.signinResponse;
import com.clement.dexwin.domain.dtos.users.SignupRequestDto;
import com.clement.dexwin.domain.models.users.Roles;
import com.clement.dexwin.domain.models.users.User;
import com.clement.dexwin.domain.repository.UserRepository;
import com.clement.dexwin.domain.security.JwtService;
import com.clement.dexwin.domain.security.SecurityUser;
import com.clement.dexwin.domain.services.contracts.AuthService;
import com.clement.dexwin.exceptions.DuplicateEmailException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.clement.dexwin.utils.ConstantMessages.DUPLICATE_EMAIL_MSG;


@Service
@AllArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public SignedUpSucessResponse register(SignupRequestDto request) {

        try {
            Optional<User> byEmail = userRepository.findByEmail(request.email());

            if (byEmail.isPresent()) {
                throw new DuplicateEmailException(DUPLICATE_EMAIL_MSG);
            }


            User user = User.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .middleName(request.middleName() != null ? request.middleName().trim() : null)
                .email(request.email().trim())
                .roles(Roles.VIEWER)
                .isActive(true)
                .password(passwordEncoder.encode(request.password()))
                .build();

            String token = jwtService.generateToken(user);

            userRepository.save(user);
            return SignedUpSucessResponse.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .token(token)
                .build();

        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException(DUPLICATE_EMAIL_MSG);
        }
    }


    @Override
    public void logout() {
        SecurityContextHolder.clearContext();
    }

    @Override
    public signinResponse login(LoginRequestDto request) {
        Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        SecurityUser securityUser = (SecurityUser) authenticate.getPrincipal();
        User user = securityUser.user();
        if (!user.isActive()) {
            throw new AuthenticationCredentialsNotFoundException("User is not verified");
        }
        String token = jwtService.generateToken(user);

        return signinResponse.builder()
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .email(user.getEmail())
            .role(user.getRoles())
            .token(token)
            .build();
    }
}