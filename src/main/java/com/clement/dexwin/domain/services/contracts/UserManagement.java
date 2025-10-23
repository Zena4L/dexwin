package com.clement.dexwin.domain.services.contracts;

import com.clement.dexwin.domain.models.users.UserListProjection;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface UserManagement {
    void deleteUser(UUID userId);

    Page<UserListProjection> getAllUser(int page, int size, String search);

    UserListProjection getUser(UUID request);
}
