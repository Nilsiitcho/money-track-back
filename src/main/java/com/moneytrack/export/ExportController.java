package com.moneytrack.export;

import com.moneytrack.common.ApiException;
import com.moneytrack.common.Months;
import com.moneytrack.security.AuthContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    /** Teto de meses por exportacao (evita range absurdo). */
    private static final int MAX_MONTHS = 120;

    private final ExportService service;
    private final ZoneId zone;

    public ExportController(ExportService service, @Value("${app.timezone}") String timezone) {
        this.service = service;
        this.zone = ZoneId.of(timezone);
    }

    /** CSV dos lancamentos no periodo [from, to] (meses YYYY-MM; default: mes atual). */
    @GetMapping(value = "/entries.csv", produces = "text/csv")
    public ResponseEntity<byte[]> entriesCsv(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        YearMonth start = from == null || from.isBlank() ? YearMonth.now(zone) : Months.parse(from);
        YearMonth end = to == null || to.isBlank() ? start : Months.parse(to);
        if (end.isBefore(start)) {
            throw ApiException.badRequest("Mes final anterior ao inicial");
        }
        if (start.until(end, java.time.temporal.ChronoUnit.MONTHS) + 1 > MAX_MONTHS) {
            throw ApiException.badRequest("Periodo muito longo (maximo " + MAX_MONTHS + " meses)");
        }

        String csv = service.buildCsv(AuthContext.currentUserId(), start, end);
        // BOM UTF-8: ajuda o Excel a reconhecer acentuacao; IAs ignoram.
        byte[] body = ("﻿" + csv).getBytes(StandardCharsets.UTF_8);
        String filename = "money-track_" + start + "_a_" + end + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }
}
