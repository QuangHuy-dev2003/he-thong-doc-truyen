package com.meobeo.truyen.repository;

import com.meobeo.truyen.domain.entity.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {

    Optional<Genre> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT g FROM Genre g WHERE g.name LIKE %:keyword% ORDER BY g.name")
    Page<Genre> findByNameContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT g FROM Genre g ORDER BY g.name")
    Page<Genre> findAllOrderByName(Pageable pageable);

    @Query("SELECT g FROM Genre g LEFT JOIN FETCH g.stories WHERE g.id = :id")
    Optional<Genre> findByIdWithStories(@Param("id") Long id);

    @Query("SELECT g FROM Genre g LEFT JOIN FETCH g.stories")
    List<Genre> findAllWithStories();

    @Query("SELECT g FROM Genre g LEFT JOIN FETCH g.stories WHERE g.name LIKE %:keyword% ORDER BY g.name")
    List<Genre> findByNameContainingWithStories(@Param("keyword") String keyword);
}