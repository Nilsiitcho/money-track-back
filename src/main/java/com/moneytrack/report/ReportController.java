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
            @RequestParam(defaultValue = "12") int months) {
        YearMonth start = from == null ? YearMonth.now(zone) : Months.parse(from);
        int cols = Math.max(1, Math.min(months, 24));
        return service.buildGrid(AuthContext.currentUserId(), start, cols);
    }
}
