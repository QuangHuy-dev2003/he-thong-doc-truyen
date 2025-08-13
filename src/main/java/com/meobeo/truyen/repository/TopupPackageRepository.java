package com.meobeo.truyen.repository;

import com.meobeo.truyen.domain.entity.TopupPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopupPackageRepository extends JpaRepository<TopupPackage, Long> {

    /**
     * Lấy danh sách tất cả gói cước đang hoạt động
     */
    @Query("SELECT tp FROM TopupPackage tp WHERE tp.isActive = true ORDER BY tp.amount ASC")
    List<TopupPackage> findByIsActiveTrue();

    /**
     * Tìm gói cước theo ID và đang hoạt động
     */
    Optional<TopupPackage> findByIdAndIsActiveTrue(Long id);

    /**
     * Kiểm tra xem có gói cước nào đang hoạt động không
     */
    boolean existsByIsActiveTrue();
}
