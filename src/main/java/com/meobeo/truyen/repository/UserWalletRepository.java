package com.meobeo.truyen.repository;

import com.meobeo.truyen.domain.entity.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserWalletRepository extends JpaRepository<UserWallet, Long> {

    /**
     * Tìm ví theo user ID
     */
    Optional<UserWallet> findByUserId(Long userId);
}
