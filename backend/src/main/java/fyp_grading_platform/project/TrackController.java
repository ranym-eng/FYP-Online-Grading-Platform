package fyp_grading_platform.project;

import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.common.exception.BusinessException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tracks")
public class TrackController {
    private final TrackRepository repository;
    public TrackController(TrackRepository repository) { this.repository = repository; }

    @PostMapping ApiResponse<Track> create(@Valid @RequestBody TrackRequest request) {
        if (repository.existsByCode(request.code())) throw new BusinessException("DUPLICATE_TRACK", "Track code already exists");
        Track t = new Track(); t.setCode(request.code()); t.setName(request.name()); t.setDescription(request.description());
        return ApiResponse.ok("Track created", repository.save(t));
    }
    @GetMapping ApiResponse<?> all() { return ApiResponse.ok("Tracks", repository.findAll()); }
    @GetMapping("/{id}") ApiResponse<?> one(@PathVariable UUID id) { return ApiResponse.ok("Track", repository.findById(id)); }
    @GetMapping("/code/{code}") ApiResponse<?> byCode(@PathVariable String code) { return ApiResponse.ok("Track", repository.findByCode(code)); }
    @PutMapping("/{id}") ApiResponse<Track> update(@PathVariable UUID id, @Valid @RequestBody TrackRequest request) {
        Track t = repository.findById(id).orElseThrow(() -> new BusinessException("TRACK_NOT_FOUND", "Track not found"));
        t.setCode(request.code()); t.setName(request.name()); t.setDescription(request.description()); return ApiResponse.ok("Track updated", repository.save(t));
    }
    @PatchMapping("/{id}/activate") ApiResponse<Track> activate(@PathVariable UUID id) { Track t = repository.findById(id).orElseThrow(); t.setActive(true); return ApiResponse.ok("Track activated", repository.save(t)); }
    @PatchMapping("/{id}/deactivate") ApiResponse<Track> deactivate(@PathVariable UUID id) { Track t = repository.findById(id).orElseThrow(); t.setActive(false); return ApiResponse.ok("Track deactivated", repository.save(t)); }
    @DeleteMapping("/{id}") ApiResponse<Void> delete(@PathVariable UUID id) { repository.deleteById(id); return ApiResponse.ok("Track deleted", null); }
}
