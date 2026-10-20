package com.moneytrack.category;

import com.moneytrack.common.ApiException;
import com.moneytrack.entry.EntryOverrideRepository;
import com.moneytrack.entry.EntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categories;
    private final EntryRepository entries;
    private final EntryOverrideRepository overrides;

    public CategoryService(CategoryRepository categories, EntryRepository entries,
                           EntryOverrideRepository overrides) {
        this.categories = categories;
        this.entries = entries;
        this.overrides = overrides;
    }

    public List<Category> list(Long userId, boolean includeArchived) {
        return includeArchived
                ? categories.findByUserIdOrderByNameAsc(userId)
                : categories.findByUserIdAndArchivedFalseOrderByNameAsc(userId);
    }

    public Category get(Long userId, Long id) {
        return categories.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Categoria nao encontrada"));
    }

    @Transactional
    public Category create(Long userId, String name) {
        String clean = normalize(name);
        if (categories.existsByUserIdAndNameIgnoreCase(userId, clean)) {
            throw ApiException.conflict("Ja existe uma categoria com esse nome");
        }
        Category c = new Category();
        c.setUserId(userId);
        c.setName(clean);
        return categories.save(c);
    }

    @Transactional
    public Category rename(Long userId, Long id, String name) {
        Category c = get(userId, id);
        String clean = normalize(name);
        if (!c.getName().equalsIgnoreCase(clean)
                && categories.existsByUserIdAndNameIgnoreCase(userId, clean)) {
            throw ApiException.conflict("Ja existe uma categoria com esse nome");
        }
        c.setName(clean);
        return categories.save(c);
    }

    @Transactional
    public Category archive(Long userId, Long id) {
        Category c = get(userId, id);
        c.setArchived(true);
        return categories.save(c);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Category c = get(userId, id);
        if (entries.existsByCategoryId(id) || overrides.existsByCategoryId(id)) {
            throw ApiException.conflict(
                    "Categoria em uso. Arquive-a em vez de excluir.");
        }
        categories.delete(c);
    }

    private String normalize(String name) {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) {
            throw ApiException.badRequest("Nome da categoria e obrigatorio");
        }
        return clean;
    }
}
