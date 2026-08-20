package fyp_grading_platform.user;

import fyp_grading_platform.auth.IndustryInvitationService;
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
    private final IndustryInvitationService invitations;

    public UserController(UserRepository repository, UserService service, IndustryInvitationService invitations) {
        this.repository = repository;
        this.service = service;
        this.invitations = invitations;
    }

    @PostMapping
    ApiResponse<User> create(@Valid @RequestBody UserRequest request) {
        User user = service.create(request);
        if (user.getRole() == UserRole.INDUSTRY_REPRESENTATIVE) invitations.invite(user);
        return ApiResponse.ok("User created", repository.findById(user.getId()).orElseThrow());
    }
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
    @PostMapping("/{id}/invite")
    ApiResponse<User> invite(@PathVariable UUID id) {
        User user = repository.findById(id).orElseThrow();
        invitations.invite(user);
        return ApiResponse.ok("Industry Guest invitation sent", repository.findById(id).orElseThrow());
    }
    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable UUID id) { repository.deleteById(id); return ApiResponse.ok("User deleted", null); }
    @GetMapping("/by-role/{role}")
    ApiResponse<?> byRole(@PathVariable UserRole role) { return ApiResponse.ok("Users by role", repository.findByRole(role)); }
    @GetMapping("/search")
    ApiResponse<?> search(@RequestParam String keyword) { return ApiResponse.ok("Search results", repository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword)); }
}
