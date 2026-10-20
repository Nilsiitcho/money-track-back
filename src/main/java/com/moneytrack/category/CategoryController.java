package com.moneytrack.category;

import com.moneytrack.security.AuthContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoryResponse> list(
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return service.list(AuthContext.currentUserId(), includeArchived)
                .stream().map(CategoryResponse::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest req) {
        return CategoryResponse.of(service.create(AuthContext.currentUserId(), req.name()));
    }

    @PutMapping("/{id}")
    public CategoryResponse rename(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        return CategoryResponse.of(service.rename(AuthContext.currentUserId(), id, req.name()));
    }

    @PostMapping("/{id}/archive")
    public CategoryResponse archive(@PathVariable Long id) {
        return CategoryResponse.of(service.archive(AuthContext.currentUserId(), id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(AuthContext.currentUserId(), id);
    }

    public record CategoryRequest(
            @NotBlank(message = "Nome da categoria e obrigatorio")
            @Size(max = 100, message = "Nome deve ter no maximo 100 caracteres")
            String name) {
    }

    public record CategoryResponse(Long id, String name, boolean archived) {
        public static CategoryResponse of(Category c) {
            return new CategoryResponse(c.getId(), c.getName(), c.isArchived());
        }
    }
}
