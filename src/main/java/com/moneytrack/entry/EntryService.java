package com.moneytrack.entry;

import com.moneytrack.category.Category;
import com.moneytrack.category.CategoryRepository;
import com.moneytrack.common.ApiException;
import com.moneytrack.common.Months;
import com.moneytrack.entry.domain.*;
import com.moneytrack.entry.dto.EntryCreateRequest;
import com.moneytrack.entry.dto.EntryUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class EntryService {

    private final EntryRepository entries;
    private final EntryOverrideRepository overrides;
    private final CategoryRepository categories;

    public EntryService(EntryRepository entries, EntryOverrideRepository overrides,
                        CategoryRepository categories) {
        this.entries = entries;
        this.overrides = overrides;
        this.categories = categories;
    }

    // ---------- leitura auxiliar ----------

    public Map<Long, String> categoryNames(Long userId) {
        Map<Long, String> map = new HashMap<>();
        for (Category c : categories.findByUserIdOrderByNameAsc(userId)) {
            map.put(c.getId(), c.getName());
        }
        return map;
    }

    public Map<Long, String> categoryKinds(Long userId) {
        Map<Long, String> map = new HashMap<>();
        for (Category c : categories.findByUserIdOrderByNameAsc(userId)) {
            map.put(c.getId(), c.getKind().name());
        }
        return map;
    }

    public List<Entry> userEntries(Long userId) {
        return entries.findByUserId(userId);
    }

    public Map<Long, Map<YearMonth, EntryOverride>> overridesByEntry(List<Entry> userEntries) {
        if (userEntries.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = userEntries.stream().map(Entry::getId).toList();
        Map<Long, Map<YearMonth, EntryOverride>> map = new HashMap<>();
        for (EntryOverride ov : overrides.findByEntryIdIn(ids)) {
            map.computeIfAbsent(ov.getEntryId(), k -> new HashMap<>())
                    .put(YearMonth.from(ov.getReferenceMonth()), ov);
        }
        return map;
    }

    // ---------- listagem de ocorrencias ----------

    public List<Occurrence> occurrencesInMonth(Long userId, YearMonth month, EntryType type,
                                               Long categoryId, Recurrence recurrence, String q) {
        List<Entry> userEntries = entries.findByUserId(userId);
        Map<Long, Map<YearMonth, EntryOverride>> ov = overridesByEntry(userEntries);
        List<Occurrence> occ = OccurrenceCalculator.occurrencesIn(userEntries, ov, month);

        String needle = q == null ? null : q.trim().toLowerCase();
        return occ.stream()
                .filter(o -> type == null || o.type() == type)
                .filter(o -> categoryId == null || Objects.equals(o.categoryId(), categoryId))
                .filter(o -> recurrence == null || o.recurrence() == recurrence)
                .filter(o -> needle == null || needle.isEmpty()
                        || o.description().toLowerCase().contains(needle))
                .sorted(Comparator.comparing(Occurrence::type)
                        .thenComparing(Occurrence::description, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Entry getRule(Long userId, Long id) {
        return entries.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Lancamento nao encontrado"));
    }

    // ---------- criacao ----------

    @Transactional
    public Entry create(Long userId, EntryCreateRequest req) {
        Long categoryId = req.categoryId() == null
                ? null : requireUsableCategory(userId, req.categoryId()).getId();

        Entry e = new Entry();
        e.setUserId(userId);
        e.setCategoryId(categoryId);
        e.setType(req.type());
        e.setDescription(req.description().trim());
        e.setAmount(req.amount());
        e.setRecurrence(req.recurrence());
        e.setStartMonth(req.startMonth().atDay(1));

        switch (req.recurrence()) {
            case UNICO -> {
                e.setEndMonth(req.startMonth().atDay(1));
            }
            case FIXO -> {
                e.setEndMonth(null); // indeterminado
            }
            case PARCELADO -> applyParcelado(e, req);
        }
        return entries.save(e);
    }

    private void applyParcelado(Entry e, EntryCreateRequest req) {
        YearMonth start = req.startMonth();
        boolean hasInstallments = req.installments() != null;
        boolean hasEnd = req.endMonth() != null;
        if (hasInstallments == hasEnd) {
            throw ApiException.badRequest(
                    "Para parcelado, informe a quantidade de parcelas OU o mes de termino (apenas um).");
        }
        int total;
        YearMonth end;
        if (hasInstallments) {
            total = req.installments();
            end = start.plusMonths(total - 1L);
        } else {
            end = req.endMonth();
            if (end.isBefore(start)) {
                throw ApiException.badRequest("Mes de termino anterior ao mes inicial");
            }
            total = (int) start.until(end, ChronoUnit.MONTHS) + 1;
        }
        e.setInstallmentFirst(1);
        e.setInstallmentTotal(total);
        e.setEndMonth(end.atDay(1));
    }

    // ---------- edicao ----------

    @Transactional
    public Entry update(Long userId, Long id, EntryUpdateRequest req) {
        Entry entry = getRule(userId, id);
        Long categoryId = req.categoryId() == null
                ? null : requireUsableCategory(userId, req.categoryId()).getId();

        EditScope scope = resolveScope(entry, req.scope());
        YearMonth ref = resolveReferenceMonth(entry, scope, req.referenceMonth());

        List<EntryOverride> entryOverrides = overrides.findByEntryId(id);
        FieldValues values = new FieldValues(req.type(), req.description().trim(),
                req.amount(), categoryId);

        EditPlan plan = RecurrenceEditor.edit(entry, entryOverrides, values, scope, ref);

        entries.save(plan.master());
        if (plan.created() != null) {
            Entry created = entries.save(plan.created());
            for (EntryOverride ov : plan.reassignedOverrides()) {
                ov.setEntryId(created.getId());
                overrides.save(ov);
            }
        }
        if (plan.override() != null) {
            overrides.save(plan.override());
        }
        return plan.master();
    }

    // ---------- exclusao ----------

    @Transactional
    public void delete(Long userId, Long id, EditScope scopeParam, YearMonth refParam) {
        Entry entry = getRule(userId, id);
        EditScope scope = resolveScope(entry, scopeParam);
        YearMonth ref = resolveReferenceMonth(entry, scope, refParam);

        List<EntryOverride> entryOverrides = overrides.findByEntryId(id);
        DeletePlan plan = RecurrenceEditor.delete(entry, entryOverrides, scope, ref);

        if (plan.deleteMaster()) {
            overrides.deleteAll(entryOverrides);
            entries.delete(entry);
            return;
        }
        entries.save(plan.master());
        if (plan.override() != null) {
            overrides.save(plan.override());
        }
        if (!plan.overridesToDelete().isEmpty()) {
            overrides.deleteAll(plan.overridesToDelete());
        }
    }

    // ---------- helpers ----------

    private Category requireUsableCategory(Long userId, Long categoryId) {
        Category c = categories.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> ApiException.badRequest("Categoria inexistente"));
        if (c.isArchived()) {
            throw ApiException.badRequest("Categoria arquivada nao pode ser usada em novos lancamentos");
        }
        return c;
    }

    /** UNICO sempre ALL. Para recorrentes, scope e obrigatorio. */
    private EditScope resolveScope(Entry entry, EditScope scope) {
        if (entry.getRecurrence() == Recurrence.UNICO) {
            return EditScope.ALL;
        }
        if (scope == null) {
            throw ApiException.badRequest(
                    "Informe o alcance (scope): ONLY_THIS, THIS_AND_FUTURE ou ALL");
        }
        return scope;
    }

    /** ref so e necessaria quando scope != ALL; deve cair no intervalo da regra. */
    private YearMonth resolveReferenceMonth(Entry entry, EditScope scope, YearMonth ref) {
        if (entry.getRecurrence() == Recurrence.UNICO || scope == EditScope.ALL) {
            return YearMonth.from(entry.getStartMonth());
        }
        if (ref == null) {
            throw ApiException.badRequest("Informe referenceMonth (mes da ocorrencia)");
        }
        YearMonth start = YearMonth.from(entry.getStartMonth());
        YearMonth end = entry.getEndMonth() == null ? null : YearMonth.from(entry.getEndMonth());
        if (ref.isBefore(start) || (end != null && ref.isAfter(end))) {
            throw ApiException.badRequest("referenceMonth fora do intervalo do lancamento");
        }
        return ref;
    }
}
