package com.clement.dexwin.domain.repository;

import com.clement.dexwin.domain.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select s from User s where s.email =:email and s.isActive = true ")
    Optional<User> findUserByEmailAndIsActive(String email);

    Optional<User> findByEmail(String email);


}
