package com.meobeo.truyen.repository;

import com.meobeo.truyen.domain.entity.GiftCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GiftCodeRepository extends JpaRepository<GiftCode, Long> {

    /**
     * Tìm gift code theo mã và trạng thái active
     */
    Optional<GiftCode> findByCodeAndIsActiveTrue(String code);

    /**
     * Lấy tất cả gift codes đang active
     */
    List<GiftCode> findByIsActiveTrue();

    /**
     * Kiểm tra gift code có tồn tại không
     */
    boolean existsByCode(String code);

    /**
     * Tìm gift code theo mã (không quan tâm trạng thái)
     */
    Optional<GiftCode> findByCode(String code);
}
