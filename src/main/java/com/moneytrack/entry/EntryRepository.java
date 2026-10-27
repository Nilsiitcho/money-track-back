package com.moneytrack.entry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EntryRepository extends JpaRepository<Entry, Long> {

    List<Entry> findByUserId(Long userId);

    List<Entry> findByUserIdAndCategoryId(Long userId, Long categoryId);

    Optional<Entry> findByIdAndUserId(Long id, Long userId);

    boolean existsByCategoryId(Long categoryId);

    @Modifying
    @Query("delete from Entry e where e.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
