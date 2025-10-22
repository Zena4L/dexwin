package com.clement.dexwin.utils;

import com.clement.dexwin.domain.models.Roles;
import com.clement.dexwin.domain.models.User;
import com.clement.dexwin.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class SeedAdminUser {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    public void loadData() {
        seedUser(
            "Clement",
            "Bogyah",
            "admin@mail.com"
        );

        seedUser(
            "super",
            "admin",
            "superadmin@mail.com"
        );
    }

    private void seedUser(String firstName, String lastName, String email) {
        if (userRepository.findByEmailAndIsActive(email).isEmpty()) {
            String encodedPassword = passwordEncoder.encode("[Password1]");
            User user = User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .password(encodedPassword)
                .roles(Roles.ADMIN)
                .isActive(true)
                .build();

            userRepository.save(user);
        }

    }
}
