package com.moneytrack.export;

import com.moneytrack.entry.EntryService;
import com.moneytrack.entry.domain.Occurrence;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Exportacao dos lancamentos em CSV (long-format: uma linha por ocorrencia de
 * cada mes). Formato pensado para colar em IA / planilha: separador virgula,
 * decimal com ponto, campos de texto entre aspas quando necessario.
 */
@Service
public class ExportService {

    private static final String HEADER = "mes,tipo,categoria,descricao,recorrencia,parcela,valor\n";

    private final EntryService entries;

    public ExportService(EntryService entries) {
        this.entries = entries;
    }

    public String buildCsv(Long userId, YearMonth from, YearMonth to) {
        Map<Long, String> names = entries.categoryNames(userId);
        StringBuilder sb = new StringBuilder(HEADER);

        for (YearMonth m = from; !m.isAfter(to); m = m.plusMonths(1)) {
            List<Occurrence> occ = entries.occurrencesInMonth(userId, m, null, null, null, null);
            for (Occurrence o : occ) {
                String cat = o.categoryId() == null ? "" : names.getOrDefault(o.categoryId(), "");
                String parcela = o.installmentLabel() == null ? "" : o.installmentLabel();
                sb.append(m).append(',')
                        .append(o.type()).append(',')
                        .append(csv(cat)).append(',')
                        .append(csv(o.description())).append(',')
                        .append(o.recurrence()).append(',')
                        .append(csv(parcela)).append(',')
                        .append(o.amount().toPlainString()).append('\n');
            }
        }
        return sb.toString();
    }

    /** Escapa um campo de texto para CSV (aspas se tiver virgula, aspas ou quebra de linha). */
    private static String csv(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
