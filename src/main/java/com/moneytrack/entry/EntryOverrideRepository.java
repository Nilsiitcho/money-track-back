package com.moneytrack.entry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EntryOverrideRepository extends JpaRepository<EntryOverride, Long> {

    List<EntryOverride> findByEntryIdIn(List<Long> entryIds);

    List<EntryOverride> findByEntryId(Long entryId);

    Optional<EntryOverride> findByEntryIdAndReferenceMonth(Long entryId, LocalDate referenceMonth);

    boolean existsByCategoryId(Long categoryId);

    @Modifying
    @Query("delete from EntryOverride o where o.entryId in (select e.id from Entry e where e.userId = :userId)")
    void deleteByUserId(@Param("userId") Long userId);
}
