package com.backend.server.model.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.backend.server.model.entity.User;
import com.backend.server.model.entity.enums.Role;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    Optional<User> findByStudentNumber(String studentNumber);
    List<User> findByRole(Role role);
    List<User> findByRoleIn(Role... roles);
    List<User> findByRoleInOrderByNameAsc(Collection<Role> roles);
}
