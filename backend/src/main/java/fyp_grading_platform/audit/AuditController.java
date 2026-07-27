package fyp_grading_platform.audit;

import fyp_grading_platform.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
public class AuditController {
    private final AuditLogRepository repository;
    public AuditController(AuditLogRepository repository) { this.repository = repository; }
    @GetMapping ApiResponse<?> all() { return ApiResponse.ok("Audit logs", repository.findAll()); }
    @GetMapping("/{id}") ApiResponse<?> one(@PathVariable UUID id) { return ApiResponse.ok("Audit log", repository.findById(id)); }
    @GetMapping("/by-user/{userId}") ApiResponse<?> byUser(@PathVariable UUID userId) { return ApiResponse.ok("Audit logs", repository.findByUserId(userId)); }
    @GetMapping("/by-entity/{entityType}/{entityId}") ApiResponse<?> byEntity(@PathVariable String entityType, @PathVariable UUID entityId) { return ApiResponse.ok("Audit logs", repository.findByEntityTypeAndEntityId(entityType, entityId)); }
    @GetMapping("/by-action/{action}") ApiResponse<?> byAction(@PathVariable String action) { return ApiResponse.ok("Audit logs", repository.findByAction(action)); }
}
