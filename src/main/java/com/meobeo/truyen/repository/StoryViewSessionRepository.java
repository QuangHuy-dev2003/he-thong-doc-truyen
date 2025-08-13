package com.meobeo.truyen.repository;

import com.meobeo.truyen.domain.entity.StoryViewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface StoryViewSessionRepository extends JpaRepository<StoryViewSession, Long> {

    /**
     * Kiểm tra xem session đã view story này trong khoảng thời gian chưa
     */
    @Query("SELECT COUNT(svs) > 0 FROM StoryViewSession svs " +
            "WHERE svs.storyId = :storyId AND svs.sessionId = :sessionId " +
            "AND svs.createdAt >= :since")
    boolean existsByStoryAndSessionSince(@Param("storyId") Long storyId,
            @Param("sessionId") String sessionId,
            @Param("since") LocalDateTime since);

    /**
     * Kiểm tra xem IP đã view story này trong khoảng thời gian chưa
     */
    @Query("SELECT COUNT(svs) > 0 FROM StoryViewSession svs " +
            "WHERE svs.storyId = :storyId AND svs.ipAddress = :ipAddress " +
            "AND svs.createdAt >= :since")
    boolean existsByStoryAndIpSince(@Param("storyId") Long storyId,
            @Param("ipAddress") String ipAddress,
            @Param("since") LocalDateTime since);

    /**
     * Xóa session cũ hơn cutoff
     */
    @Modifying
    @Query("DELETE FROM StoryViewSession svs WHERE svs.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
