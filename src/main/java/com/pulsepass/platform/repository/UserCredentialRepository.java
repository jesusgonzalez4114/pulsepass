package com.pulsepass.platform.repository;

import com.pulsepass.platform.domain.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
}
