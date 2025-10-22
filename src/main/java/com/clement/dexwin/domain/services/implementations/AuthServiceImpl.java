package com.clement.dexwin.domain.services.implementations;

import com.clement.dexwin.domain.dtos.SignedUpSucessResponse;
import com.clement.dexwin.domain.dtos.SignupRequestDto;
import com.clement.dexwin.domain.models.Roles;
import com.clement.dexwin.domain.models.User;
import com.clement.dexwin.domain.repository.UserRepository;
import com.clement.dexwin.domain.security.JwtService;
import com.clement.dexwin.domain.services.contracts.AuthService;
import com.clement.dexwin.exceptions.DuplicateEmailException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
}