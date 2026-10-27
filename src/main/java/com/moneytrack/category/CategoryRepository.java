package com.moneytrack.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserIdOrderByNameAsc(Long userId);

    List<Category> findByUserIdAndArchivedFalseOrderByNameAsc(Long userId);

    Optional<Category> findByIdAndUserId(Long id, Long userId);

    Optional<Category> findFirstByUserIdAndKind(Long userId, CategoryKind kind);

    List<Category> findByUserIdAndKindAndArchivedFalseOrderByNameAsc(Long userId, CategoryKind kind);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    @Modifying
    @Query("delete from Category c where c.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
