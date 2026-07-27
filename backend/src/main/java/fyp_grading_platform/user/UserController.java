package fyp_grading_platform.user;

import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.UserStatus;
import fyp_grading_platform.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository repository;
    private final UserService service;

    public UserController(UserRepository repository, UserService service) {
        this.repository = repository;
        this.service = service;
    }

    @PostMapping
    ApiResponse<User> create(@Valid @RequestBody UserRequest request) { return ApiResponse.ok("User created", service.create(request)); }
    @GetMapping
    ApiResponse<?> all() { return ApiResponse.ok("Users", repository.findAll()); }
    @GetMapping("/{id}")
    ApiResponse<?> one(@PathVariable UUID id) { return ApiResponse.ok("User", repository.findById(id)); }
    @PutMapping("/{id}")
    ApiResponse<User> update(@PathVariable UUID id, @Valid @RequestBody UserRequest request) { return ApiResponse.ok("User updated", service.update(id, request)); }
    @PatchMapping("/{id}/activate")
    ApiResponse<User> activate(@PathVariable UUID id) { return ApiResponse.ok("User activated", service.setStatus(id, UserStatus.ACTIVE)); }
    @PatchMapping("/{id}/deactivate")
    ApiResponse<User> deactivate(@PathVariable UUID id) { return ApiResponse.ok("User deactivated", service.setStatus(id, UserStatus.INACTIVE)); }
    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable UUID id) { repository.deleteById(id); return ApiResponse.ok("User deleted", null); }
    @GetMapping("/by-role/{role}")
    ApiResponse<?> byRole(@PathVariable UserRole role) { return ApiResponse.ok("Users by role", repository.findByRole(role)); }
    @GetMapping("/search")
    ApiResponse<?> search(@RequestParam String keyword) { return ApiResponse.ok("Search results", repository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword)); }
}
