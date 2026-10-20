package com.moneytrack.entry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EntryOverrideRepository extends JpaRepository<EntryOverride, Long> {

    List<EntryOverride> findByEntryIdIn(List<Long> entryIds);

    List<EntryOverride> findByEntryId(Long entryId);

    Optional<EntryOverride> findByEntryIdAndReferenceMonth(Long entryId, LocalDate referenceMonth);

    boolean existsByCategoryId(Long categoryId);
}
