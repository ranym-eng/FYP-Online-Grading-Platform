package fyp_grading_platform.project;

import java.time.LocalDateTime;

public record PhaseAccessResponse(
        boolean allowed,
        String reasonCode,
        String message,
        LocalDateTime phaseDeadline,
        LocalDateTime effectiveDeadline,
        boolean personalExtension
) {}
