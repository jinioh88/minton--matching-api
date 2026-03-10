package org.app.mintonmatchapi.user.repository;

import org.app.mintonmatchapi.user.entity.Provider;
import org.app.mintonmatchapi.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByProviderAndProviderId(Provider provider, String providerId);

    boolean existsByNickname(String nickname);
}
