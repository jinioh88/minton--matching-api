package org.app.mintonmatchapi.user.repository;

import jakarta.persistence.LockModeType;
import org.app.mintonmatchapi.user.entity.Provider;
import org.app.mintonmatchapi.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByProviderAndProviderId(Provider provider, String providerId);

    boolean existsByNickname(String nickname);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
