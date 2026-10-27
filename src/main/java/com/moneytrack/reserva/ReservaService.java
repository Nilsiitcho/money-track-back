package com.moneytrack.reserva;

import com.moneytrack.category.Category;
import com.moneytrack.category.CategoryKind;
import com.moneytrack.category.CategoryRepository;
import com.moneytrack.common.ApiException;
import com.moneytrack.entry.Entry;
import com.moneytrack.entry.EntryOverride;
import com.moneytrack.entry.EntryOverrideRepository;
import com.moneytrack.entry.EntryRepository;
import com.moneytrack.entry.EntryType;
import com.moneytrack.entry.Recurrence;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Reserva de Gastos: valor mensal para gastos variaveis, lancado automaticamente
 * como despesa FIXA na categoria especial. Calcula o orcamento diario do mes.
 */
@Service
public class ReservaService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final String DESCRIPTION = "Reserva de Gastos";

    private final CategoryRepository categories;
    private final EntryRepository entries;
    private final EntryOverrideRepository overrides;

    public ReservaService(CategoryRepository categories, EntryRepository entries,
                          EntryOverrideRepository overrides) {
        this.categories = categories;
        this.entries = entries;
        this.overrides = overrides;
    }

    /** Orcamento diario = valor / dias do mes (2 casas, HALF_UP). Logica pura. */
    public static BigDecimal dailyBudget(BigDecimal amount, int daysInMonth) {
        if (amount == null || amount.signum() <= 0 || daysInMonth <= 0) {
            return ZERO;
        }
        return amount.divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);
    }

    public ReservaResponse get(Long userId, YearMonth month) {
        Category cat = requireReservaCategory(userId);
        BigDecimal amount = currentEntry(userId, cat.getId()).map(Entry::getAmount).orElse(ZERO);
        int days = month.lengthOfMonth();
        return new ReservaResponse(cat.getId(), amount, month.toString(), days, dailyBudget(amount, days));
    }

    /** Painel do lancamento rapido (antes de lancar) para a data informada. */
    public ReservaStatus status(Long userId, LocalDate date) {
        Category cat = requireReservaCategory(userId);
        YearMonth month = YearMonth.from(date);
        BigDecimal reserva = currentEntry(userId, cat.getId()).map(Entry::getAmount).orElse(ZERO);
        BigDecimal spent = spentInMonth(userId, cat.getId(), month);
        int days = month.lengthOfMonth();
        int remainingDays = days - date.getDayOfMonth() + 1;
        BigDecimal remainingMonth = reserva.subtract(spent);
        BigDecimal daily = remainingDays > 0
                ? remainingMonth.divide(BigDecimal.valueOf(remainingDays), 2, RoundingMode.HALF_UP)
                : remainingMonth;
        return new ReservaStatus(reserva, days, remainingDays, spent, remainingMonth, daily);
    }

    /** Lanca um gasto rapido (despesa UNICO na reserva). Retorna o status atualizado. */
    @Transactional
    public ReservaStatus quickGasto(Long userId, BigDecimal amount, LocalDate date, String description) {
        if (amount == null || amount.signum() <= 0) {
            throw ApiException.badRequest("Valor do gasto deve ser maior que zero");
        }
        Category cat = requireReservaCategory(userId);
        YearMonth month = YearMonth.from(date);

        Entry e = new Entry();
        e.setUserId(userId);
        e.setCategoryId(cat.getId());
        e.setType(EntryType.DESPESA);
        String desc = description == null || description.isBlank() ? "Gasto rapido" : description.trim();
        e.setDescription(desc);
        e.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        e.setRecurrence(Recurrence.UNICO);
        e.setStartMonth(month.atDay(1));
        e.setEndMonth(month.atDay(1));
        entries.save(e);

        return status(userId, date);
    }

    private BigDecimal spentInMonth(Long userId, Long categoryId, YearMonth month) {
        BigDecimal total = ZERO;
        for (Entry e : entries.findByUserIdAndCategoryId(userId, categoryId)) {
            if (e.getRecurrence() == Recurrence.UNICO
                    && YearMonth.from(e.getStartMonth()).equals(month)) {
                total = total.add(e.getAmount());
            }
        }
        return total;
    }

    @Transactional
    public ReservaResponse set(Long userId, BigDecimal amount, YearMonth fromMonth, YearMonth viewMonth) {
        if (amount == null || amount.signum() < 0) {
            throw ApiException.badRequest("Valor da reserva nao pode ser negativo");
        }
        Category cat = requireReservaCategory(userId);
        Optional<Entry> existing = currentEntry(userId, cat.getId());

        if (amount.signum() == 0) {
            existing.ifPresent(this::removeEntry);
        } else if (existing.isPresent()) {
            Entry e = existing.get();
            e.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
            entries.save(e);
        } else {
            entries.save(newFixedEntry(userId, cat.getId(), amount, fromMonth));
        }
        return get(userId, viewMonth);
    }

    private Entry newFixedEntry(Long userId, Long categoryId, BigDecimal amount, YearMonth fromMonth) {
        Entry e = new Entry();
        e.setUserId(userId);
        e.setCategoryId(categoryId);
        e.setType(EntryType.DESPESA);
        e.setDescription(DESCRIPTION);
        e.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        e.setRecurrence(Recurrence.FIXO);
        e.setStartMonth(fromMonth.atDay(1));
        e.setEndMonth(null);
        return e;
    }

    private void removeEntry(Entry e) {
        List<EntryOverride> ovs = overrides.findByEntryId(e.getId());
        if (!ovs.isEmpty()) {
            overrides.deleteAll(ovs);
        }
        entries.delete(e);
    }

    private Optional<Entry> currentEntry(Long userId, Long categoryId) {
        return entries.findByUserIdAndCategoryId(userId, categoryId).stream()
                .filter(e -> e.getRecurrence() == Recurrence.FIXO)
                .findFirst();
    }

    /** Retorna a categoria especial de reserva, criando-a se nao existir (ex.: apos reset). */
    private Category requireReservaCategory(Long userId) {
        return categories.findFirstByUserIdAndKind(userId, CategoryKind.RESERVA)
                .orElseGet(() -> {
                    Category c = new Category();
                    c.setUserId(userId);
                    c.setName("Reserva");
                    c.setKind(CategoryKind.RESERVA);
                    return categories.save(c);
                });
    }
}
