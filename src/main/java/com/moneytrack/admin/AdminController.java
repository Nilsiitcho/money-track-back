package com.moneytrack.admin;

import com.moneytrack.security.AuthContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ResetService service;

    public AdminController(ResetService service) {
        this.service = service;
    }

    /** Reseta dados do usuario atual. Exige a senha de login no corpo. */
    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody ResetRequest req) {
        service.reset(AuthContext.currentUserId(), req.scope(), req.password());
    }
}
