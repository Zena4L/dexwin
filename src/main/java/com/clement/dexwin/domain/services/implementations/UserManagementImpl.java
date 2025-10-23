package com.clement.dexwin.domain.services.implementations;

import com.clement.dexwin.domain.models.users.UserListProjection;
import com.clement.dexwin.domain.repository.UserRepository;
import com.clement.dexwin.domain.services.contracts.UserManagement;
import com.clement.dexwin.exceptions.BadRequestException;
import com.clement.dexwin.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.clement.dexwin.utils.ConstantMessages.INVALID_PAGE_NUMBER;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementImpl implements UserManagement {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        log.info("deleting user with id {}", userId);
        userRepository.deleteUserById(userId);
    }

    @Override
    public Page<UserListProjection> getAllUser(int page, int size, String search) {
        if (page < 0) {
            throw new BadRequestException(INVALID_PAGE_NUMBER);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        return userRepository.findAllUserByProjection(pageable, search.trim());
    }

    @Override
    public UserListProjection getUser(UUID request) {
        return userRepository.findUserById(request).orElseThrow(() -> new NotFoundException("User not found"));
    }
}
