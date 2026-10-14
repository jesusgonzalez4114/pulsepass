package com.pulsepass.platform.repository;

import com.pulsepass.platform.domain.Role;
import com.pulsepass.platform.domain.UserRole;
import com.pulsepass.platform.domain.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    @Query("select r.id.role from UserRole r where r.id.userId = :userId")
    List<Role> findRolesByUserId(@Param("userId") Long userId);
}
