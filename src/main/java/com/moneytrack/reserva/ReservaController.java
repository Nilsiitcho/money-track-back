package com.moneytrack.reserva;

import com.moneytrack.common.ApiException;
import com.moneytrack.common.Months;
import com.moneytrack.security.AuthContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/reserva")
public class ReservaController {

    private final ReservaService service;
    private final ZoneId zone;

    public ReservaController(ReservaService service, @Value("${app.timezone}") String timezone) {
        this.service = service;
        this.zone = ZoneId.of(timezone);
    }

    /** Estado da reserva para 'month' (default: mes atual). */
    @GetMapping
    public ReservaResponse get(@RequestParam(required = false) String month) {
        YearMonth m = month == null ? YearMonth.now(zone) : Months.parse(month);
        return service.get(AuthContext.currentUserId(), m);
    }

    /** Define o valor da reserva (lancado como FIXO). Retorna o estado do mes informado. */
    @PutMapping
    public ReservaResponse set(@Valid @RequestBody ReservaRequest req,
                               @RequestParam(required = false) String month) {
        YearMonth view = month == null ? YearMonth.now(zone) : Months.parse(month);
        YearMonth from = req.fromMonth() == null ? null : Months.parse(req.fromMonth());
        return service.set(AuthContext.currentUserId(), req.amount(), from, view);
    }

    /** Painel do lancamento rapido para a data (default: hoje). */
    @GetMapping("/status")
    public ReservaStatus status(@RequestParam(required = false) String date) {
        return service.status(AuthContext.currentUserId(), parseDate(date));
    }

    /** Lanca um gasto rapido (despesa que consome a reserva). */
    @PostMapping("/gastos")
    public ReservaStatus gasto(@Valid @RequestBody GastoRequest req) {
        // Mes base (default: mes atual). Credito lanca no mes seguinte; debito no proprio mes.
        YearMonth base = req.month() == null || req.month().isBlank()
                ? YearMonth.now(zone) : Months.parse(req.month());
        boolean credit = req.credit() == null || req.credit();
        YearMonth target = credit ? base.plusMonths(1) : base;

        Long userId = AuthContext.currentUserId();
        service.quickGasto(userId, req.amount(), target, req.description());

        // Status do mes de destino: no mes atual usa hoje (dias restantes reais), senao dia 1.
        LocalDate statusDate = target.equals(YearMonth.now(zone)) ? LocalDate.now(zone) : target.atDay(1);
        return service.status(userId, statusDate);
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return LocalDate.now(zone);
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (DateTimeException ex) {
            throw ApiException.badRequest("Data invalida '" + date + "' (use YYYY-MM-DD)");
        }
    }

    public record GastoRequest(
            @NotNull(message = "Valor e obrigatorio")
            @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
            BigDecimal amount,
            String month,
            String description,
            Boolean credit) {
    }
}
