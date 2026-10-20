package com.moneytrack.entry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntryRepository extends JpaRepository<Entry, Long> {

    List<Entry> findByUserId(Long userId);

    Optional<Entry> findByIdAndUserId(Long id, Long userId);

    boolean existsByCategoryId(Long categoryId);
}
