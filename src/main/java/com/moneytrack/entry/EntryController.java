package com.moneytrack.entry;

import com.moneytrack.common.Months;
import com.moneytrack.entry.domain.Occurrence;
import com.moneytrack.entry.dto.EntryCreateRequest;
import com.moneytrack.entry.dto.EntryResponse;
import com.moneytrack.entry.dto.EntryUpdateRequest;
import com.moneytrack.entry.dto.OccurrenceResponse;
import com.moneytrack.security.AuthContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/entries")
public class EntryController {

    private final EntryService service;

    public EntryController(EntryService service) {
        this.service = service;
    }

    /** Lista ocorrencias de um mes com filtros. */
    @GetMapping
    public List<OccurrenceResponse> list(
            @RequestParam String month,
            @RequestParam(required = false) EntryType type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Recurrence recurrence,
            @RequestParam(required = false) String q) {

        Long userId = AuthContext.currentUserId();
        YearMonth ym = Months.parse(month);
        Map<Long, String> names = service.categoryNames(userId);
        Map<Long, String> kinds = service.categoryKinds(userId);
        List<Occurrence> occ = service.occurrencesInMonth(userId, ym, type, categoryId, recurrence, q);
        return occ.stream()
                .map(o -> {
                    // Gasto rapido = ocorrencia UNICO na categoria especial RESERVA.
                    boolean quick = o.recurrence() == Recurrence.UNICO
                            && "RESERVA".equals(kinds.get(o.categoryId()));
                    return OccurrenceResponse.of(o, names.get(o.categoryId()), quick);
                })
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EntryResponse create(@Valid @RequestBody EntryCreateRequest req) {
        Long userId = AuthContext.currentUserId();
        Entry e = service.create(userId, req);
        return EntryResponse.of(e, service.categoryNames(userId).get(e.getCategoryId()));
    }

    @GetMapping("/{id}")
    public EntryResponse get(@PathVariable Long id) {
        Long userId = AuthContext.currentUserId();
        Entry e = service.getRule(userId, id);
        return EntryResponse.of(e, service.categoryNames(userId).get(e.getCategoryId()));
    }

    @PutMapping("/{id}")
    public EntryResponse update(@PathVariable Long id, @Valid @RequestBody EntryUpdateRequest req) {
        Long userId = AuthContext.currentUserId();
        Entry e = service.update(userId, id, req);
        return EntryResponse.of(e, service.categoryNames(userId).get(e.getCategoryId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
                       @RequestParam(required = false) EditScope scope,
                       @RequestParam(required = false) String referenceMonth) {
        Long userId = AuthContext.currentUserId();
        YearMonth ref = referenceMonth == null ? null : Months.parse(referenceMonth);
        service.delete(userId, id, scope, ref);
    }
}
