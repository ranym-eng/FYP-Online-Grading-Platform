package fyp_grading_platform.notification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailRequest(@Email @NotBlank String recipient, @NotBlank String subject, String body, String attachmentPath) {}
