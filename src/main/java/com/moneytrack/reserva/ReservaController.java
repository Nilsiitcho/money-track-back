package com.moneytrack.reserva;

import com.moneytrack.common.Months;
import com.moneytrack.security.AuthContext;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

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
        YearMonth from = req.fromMonth() == null ? view : Months.parse(req.fromMonth());
        return service.set(AuthContext.currentUserId(), req.amount(), from, view);
    }
}
