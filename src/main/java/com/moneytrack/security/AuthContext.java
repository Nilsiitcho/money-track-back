package com.moneytrack.security;

import com.moneytrack.common.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Acesso ao usuario autenticado atual. */
public final class AuthContext {

    private AuthContext() {
    }

    public static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long uid)) {
            throw ApiException.notFound("Nao autenticado");
        }
        return uid;
    }
}
