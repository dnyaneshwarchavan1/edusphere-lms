package com.edusphere.lms.repository;

import com.edusphere.lms.entity.User;
import com.edusphere.lms.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAllByRole(Role role);
}
