package com.moneytrack.admin;

import com.moneytrack.category.CategoryRepository;
import com.moneytrack.common.ApiException;
import com.moneytrack.entry.EntryOverrideRepository;
import com.moneytrack.entry.EntryRepository;
import com.moneytrack.user.User;
import com.moneytrack.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reset de dados do usuario, protegido por senha. */
@Service
public class ResetService {

    private final EntryOverrideRepository overrides;
    private final EntryRepository entries;
    private final CategoryRepository categories;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public ResetService(EntryOverrideRepository overrides, EntryRepository entries,
                        CategoryRepository categories, UserRepository users, PasswordEncoder encoder) {
        this.overrides = overrides;
        this.entries = entries;
        this.categories = categories;
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional
    public void reset(Long userId, ResetScope scope, String password) {
        verifyPassword(userId, password);
        // Ordem respeita as FKs: override -> entry -> category.
        overrides.deleteByUserId(userId);
        entries.deleteByUserId(userId);
        if (scope == ResetScope.ENTRIES_AND_CATEGORIES) {
            categories.deleteByUserId(userId);
        }
    }

    private void verifyPassword(Long userId, String password) {
        User user = users.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("Nao autenticado"));
        if (password == null || !encoder.matches(password, user.getPasswordHash())) {
            throw ApiException.unauthorized("Senha invalida");
        }
    }
}
