package com.moneytrack.report;

import com.moneytrack.entry.Entry;
import com.moneytrack.entry.EntryOverride;
import com.moneytrack.entry.EntryService;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final EntryService entryService;

    public ReportService(EntryService entryService) {
        this.entryService = entryService;
    }

    public GridResponse buildGrid(Long userId, YearMonth from, int months) {
        List<Entry> entries = entryService.userEntries(userId);
        Map<Long, Map<YearMonth, EntryOverride>> overrides = entryService.overridesByEntry(entries);
        Map<Long, String> names = entryService.categoryNames(userId);
        return GridCalculator.build(entries, overrides, names, from, months);
    }
}
