package com.moneytrack.dashboard;

import com.moneytrack.entry.Entry;
import com.moneytrack.entry.EntryOverride;
import com.moneytrack.entry.EntryService;
import com.moneytrack.entry.EntryType;
import com.moneytrack.entry.domain.BalanceCalculator;
import com.moneytrack.entry.domain.MonthBalance;
import com.moneytrack.entry.domain.Occurrence;
import com.moneytrack.entry.domain.OccurrenceCalculator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;

@Service
public class DashboardService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final EntryService entryService;

    public DashboardService(EntryService entryService) {
        this.entryService = entryService;
    }

    public DashboardResponse build(Long userId, YearMonth month) {
        List<Entry> entries = entryService.userEntries(userId);
        Map<Long, Map<YearMonth, EntryOverride>> overrides = entryService.overridesByEntry(entries);
        Map<Long, String> names = entryService.categoryNames(userId);

        MonthBalance totals = BalanceCalculator.monthTotals(entries, overrides, month);
        List<MonthBalance> series = BalanceCalculator.series(entries, overrides, month);
        BigDecimal accumulated = series.isEmpty() ? ZERO : series.get(series.size() - 1).accumulated();

        List<DashboardResponse.SeriesPoint> points = series.stream()
                .map(mb -> new DashboardResponse.SeriesPoint(
                        mb.month().toString(), mb.saldo(), mb.accumulated()))
                .toList();

        // despesas por categoria no mes
        List<Occurrence> occ = OccurrenceCalculator.occurrencesIn(entries, overrides, month);
        Map<Long, BigDecimal> byCat = new LinkedHashMap<>();
        for (Occurrence o : occ) {
            if (o.type() == EntryType.DESPESA) {
                byCat.merge(o.categoryId(), o.amount(), BigDecimal::add);
            }
        }
        List<DashboardResponse.CategorySlice> slices = byCat.entrySet().stream()
                .map(e -> new DashboardResponse.CategorySlice(
                        e.getKey(), names.getOrDefault(e.getKey(), "?"), e.getValue()))
                .sorted(Comparator.comparing(DashboardResponse.CategorySlice::total).reversed())
                .toList();

        return new DashboardResponse(
                month.toString(),
                new DashboardResponse.Totals(totals.receitas(), totals.despesas(), totals.saldo()),
                accumulated,
                points,
                slices);
    }
}
