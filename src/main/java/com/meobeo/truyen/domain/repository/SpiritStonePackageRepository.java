package com.meobeo.truyen.domain.repository;

import com.meobeo.truyen.domain.entity.SpiritStonePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpiritStonePackageRepository extends JpaRepository<SpiritStonePackage, Long> {

    /**
     * Lấy tất cả gói đang hoạt động
     */
    List<SpiritStonePackage> findByIsActiveTrue();

    /**
     * Lấy gói theo ID và đang hoạt động
     */
    Optional<SpiritStonePackage> findByIdAndIsActiveTrue(Long id);

    /**
     * Kiểm tra có gói nào đang hoạt động không
     */
    boolean existsByIsActiveTrue();
}
