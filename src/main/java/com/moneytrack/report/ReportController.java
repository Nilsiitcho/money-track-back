package com.moneytrack.report;

import com.moneytrack.common.Months;
import com.moneytrack.security.AuthContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService service;
    private final ZoneId zone;

    public ReportController(ReportService service, @Value("${app.timezone}") String timezone) {
        this.service = service;
        this.zone = ZoneId.of(timezone);
    }

    /**
     * Tabela pivo de 'months' colunas a partir de 'from' (default: mes atual).
     */
    @GetMapping("/grid")
    public GridResponse grid(
            @RequestParam(required = false) String from,
            @RequestParam(defaultValue = "12") int months,
            @RequestParam(required = false) String exclude) {
        YearMonth start = from == null ? YearMonth.now(zone) : Months.parse(from);
        int cols = Math.max(1, Math.min(months, 24));
        return service.buildGrid(AuthContext.currentUserId(), start, cols, parseExclude(exclude));
    }

    /** Converte "1,2,3" (ids de lancamentos a ocultar na simulacao) em conjunto. */
    private static Set<Long> parseExclude(String exclude) {
        if (exclude == null || exclude.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(exclude.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
