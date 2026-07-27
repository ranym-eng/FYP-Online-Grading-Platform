package fyp_grading_platform.project;

import fyp_grading_platform.common.ExtensionRequestStatus;
import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/phase-extension-requests")
public class PhaseExtensionController {
    private final PhaseExtensionService extensions;
    private final CurrentUserService currentUsers;

    public PhaseExtensionController(PhaseExtensionService extensions, CurrentUserService currentUsers) {
        this.extensions = extensions;
        this.currentUsers = currentUsers;
    }

    @PostMapping
    ApiResponse<?> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody PhaseExtensionCreateRequest request
    ) {
        return ApiResponse.ok("Extension request submitted", extensions.create(currentUsers.requireUser(authorization), request));
    }

    @GetMapping("/my")
    ApiResponse<?> mine(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.ok("My extension requests", extensions.mine(currentUsers.requireUser(authorization)));
    }

    @GetMapping
    ApiResponse<?> all(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) ExtensionRequestStatus status
    ) {
        currentUsers.requireAdmin(authorization);
        return ApiResponse.ok("Extension requests", extensions.all(status));
    }

    @PatchMapping("/{id}/approve")
    ApiResponse<?> approve(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id,
            @Valid @RequestBody PhaseExtensionDecisionRequest request
    ) {
        return ApiResponse.ok("Extension request approved", extensions.approve(id, request, currentUsers.requireAdmin(authorization)));
    }

    @PatchMapping("/{id}/reject")
    ApiResponse<?> reject(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id,
            @Valid @RequestBody PhaseExtensionDecisionRequest request
    ) {
        return ApiResponse.ok("Extension request rejected", extensions.reject(id, request, currentUsers.requireAdmin(authorization)));
    }
}
