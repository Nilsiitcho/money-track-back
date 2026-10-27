package com.moneytrack.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResetRequest(
        @NotNull(message = "Informe o escopo do reset") ResetScope scope,
        @NotBlank(message = "Informe a senha") String password) {
}
