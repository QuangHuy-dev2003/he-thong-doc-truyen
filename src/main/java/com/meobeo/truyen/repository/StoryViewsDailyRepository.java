package com.meobeo.truyen.repository;

import com.meobeo.truyen.domain.entity.StoryViewsDaily;
import com.meobeo.truyen.domain.entity.StoryViewsDailyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StoryViewsDailyRepository extends JpaRepository<StoryViewsDaily, StoryViewsDailyId> {

    /**
     * Tăng views theo ngày cho 1 truyện bằng upsert (PostgreSQL)
     */
    @Modifying
    @Query(value = "INSERT INTO story_views_daily (story_id, view_date, views) VALUES (:storyId, :viewDate, 1) " +
            "ON CONFLICT (story_id, view_date) DO UPDATE SET views = story_views_daily.views + 1", nativeQuery = true)
    void upsertIncrement(@Param("storyId") Long storyId, @Param("viewDate") LocalDate viewDate);

    /**
     * Lấy views theo ngày trong khoảng [start, end] cho 1 truyện
     */
    @Query("SELECT svd.id.viewDate AS period, SUM(svd.views) AS views " +
            "FROM StoryViewsDaily svd " +
            "WHERE svd.story.id = :storyId AND svd.id.viewDate BETWEEN :start AND :end " +
            "GROUP BY svd.id.viewDate ORDER BY svd.id.viewDate ASC")
    List<DateViewsProjection> findDailyViews(@Param("storyId") Long storyId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /**
     * Lấy views gộp theo tuần (PostgreSQL date_trunc)
     */
    @Query(value = "SELECT (date_trunc('week', view_date))::date AS period, SUM(views) AS views " +
            "FROM story_views_daily " +
            "WHERE story_id = :storyId AND view_date BETWEEN :start AND :end " +
            "GROUP BY period ORDER BY period ASC", nativeQuery = true)
    List<DateViewsProjection> findWeeklyViews(@Param("storyId") Long storyId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /**
     * Lấy views gộp theo tháng (PostgreSQL date_trunc)
     */
    @Query(value = "SELECT (date_trunc('month', view_date))::date AS period, SUM(views) AS views " +
            "FROM story_views_daily " +
            "WHERE story_id = :storyId AND view_date BETWEEN :start AND :end " +
            "GROUP BY period ORDER BY period ASC", nativeQuery = true)
    List<DateViewsProjection> findMonthlyViews(@Param("storyId") Long storyId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /**
     * Top stories theo views trong khoảng thời gian
     */
    @Query(value = "SELECT story_id AS storyId, SUM(views) AS totalViews " +
            "FROM story_views_daily " +
            "WHERE view_date BETWEEN :start AND :end " +
            "GROUP BY story_id ORDER BY totalViews DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<TopStoryViewsProjection> findTopStories(@Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("limit") int limit,
            @Param("offset") int offset);

    /**
     * Tổng số truyện có xuất hiện trong khoảng thời gian (phục vụ phân trang Top)
     */
    @Query(value = "SELECT COUNT(*) FROM (SELECT 1 FROM story_views_daily WHERE view_date BETWEEN :start AND :end GROUP BY story_id) t", nativeQuery = true)
    long countStoriesInRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * Xóa dữ liệu cũ hơn cutoff
     */
    @Modifying
    @Query("DELETE FROM StoryViewsDaily svd WHERE svd.id.viewDate < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDate cutoff);

    /** Projection period-views */
    interface DateViewsProjection {
        LocalDate getPeriod();

        Long getViews();
    }

    /** Projection top story */
    interface TopStoryViewsProjection {
        Long getStoryId();

        Long getTotalViews();
    }
}
