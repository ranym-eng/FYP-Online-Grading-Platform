package fyp_grading_platform.project;

import fyp_grading_platform.common.EvaluationType;

import java.util.UUID;

public record ProjectEvaluationAccess(UUID projectId, EvaluationType evaluationType) {}
