package com.clement.dexwin.domain.repository;

import com.clement.dexwin.domain.models.User;
import com.clement.dexwin.domain.models.UserByEmail;
import com.clement.dexwin.domain.models.UserListProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select s from User s where s.email =:email and s.isActive = true ")
    Optional<User> findUserByEmailAndIsActive(String email);

    Optional<User> findByEmail(String email);

    @Query("select s.id AS id from User s where s.email =:email and s.isActive = true ")
    Optional<UserByEmail> findByEmailAndIsActive(String email);

    @Modifying
    @Query("delete from User u where u.id = :userId")
    void deleteUserById(@Param("userId") UUID userId);

  @Query("""
      SELECT u.id AS id, 
             u.firstName AS firstName, 
             u.lastName AS lastName, 
             u.roles AS roles 
      FROM User u 
      WHERE u.isActive = true 
      AND u.isDeleted = false 
      AND (
          LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) 
          OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) 
          OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
      )
      """)
    Page<UserListProjection> findAllUserByProjection(Pageable pageable, @Param("search") String search);

    @Query("""
      SELECT u.id AS id, 
             u.firstName AS firstName, 
             u.lastName AS lastName, 
             u.roles AS roles 
      FROM User u 
      WHERE u.id = :userId 
      """
      )

    Optional<UserListProjection> findUserById(@Param("userId") UUID userId);
}
