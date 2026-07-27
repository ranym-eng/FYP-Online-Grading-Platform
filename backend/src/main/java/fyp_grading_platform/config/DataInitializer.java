package fyp_grading_platform.config;

import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.common.PhaseType;
import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.UserStatus;
import fyp_grading_platform.evaluation.EvaluationFormTemplate;
import fyp_grading_platform.evaluation.EvaluationFormTemplateRepository;
import fyp_grading_platform.evaluation.RubricCriterion;
import fyp_grading_platform.evaluation.RubricCriterionRepository;
import fyp_grading_platform.grading.GradeRule;
import fyp_grading_platform.grading.GradeRuleRepository;
import fyp_grading_platform.project.Track;
import fyp_grading_platform.project.TrackRepository;
import fyp_grading_platform.user.User;
import fyp_grading_platform.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedData(TrackRepository tracks, UserRepository users, PasswordEncoder encoder, GradeRuleRepository gradeRules, EvaluationFormTemplateRepository forms, RubricCriterionRepository criteria) {
        return args -> {
            seedTrack(tracks, "EIC", "Electronics, Instrumentation and Control");
            seedTrack(tracks, "CSN", "Communication and Signal Networks");
            seedTrack(tracks, "CSP", "Computer Systems and Programming");
            seedTrack(tracks, "PSE", "Power Systems Engineering");

            if (!users.existsByEmailIgnoreCase("admin@squ.edu.om")) {
                User admin = new User();
                admin.setUniversityId("ADMIN-001");
                admin.setFullName("FYP Administrator");
                admin.setEmail("admin@squ.edu.om");
                admin.setPasswordHash(encoder.encode("Admin@123"));
                admin.setRole(UserRole.ADMIN);
                admin.setStatus(UserStatus.ACTIVE);
                users.save(admin);
            }

            seedRule(gradeRules, PhaseType.PHASE_I, EvaluationType.SUPERVISOR_PHASE_I, 40);
            seedRule(gradeRules, PhaseType.PHASE_I, EvaluationType.REPORT_PHASE_I, 35);
            seedRule(gradeRules, PhaseType.PHASE_I, EvaluationType.ORAL_PHASE_I, 25);
            seedRule(gradeRules, PhaseType.PHASE_II, EvaluationType.SUPERVISOR_PHASE_II, 30);
            seedRule(gradeRules, PhaseType.PHASE_II, EvaluationType.REPORT_PHASE_II, 25);
            seedRule(gradeRules, PhaseType.PHASE_II, EvaluationType.ORAL_PHASE_II, 25);
            seedRule(gradeRules, PhaseType.PHASE_II, EvaluationType.DEMO_DAY_INDUSTRY, 20);

            for (EvaluationType type : EvaluationType.values()) {
                forms.findFirstByEvaluationTypeAndActiveTrue(type).orElseGet(() -> {
                    EvaluationFormTemplate form = new EvaluationFormTemplate();
                    form.setName(type.name().replace('_', ' '));
                    form.setEvaluationType(type);
                    form.setPhaseType(type.name().endsWith("PHASE_I") ? PhaseType.PHASE_I : PhaseType.PHASE_II);
                    form.setDescription("Default rubric generated from project specification");
                    form.setTotalWeight(100);
                    EvaluationFormTemplate saved = forms.save(form);
                    seedCriterion(criteria, saved, "Technical quality", 100, 0.4, 1);
                    seedCriterion(criteria, saved, "Report / presentation clarity", 100, 0.3, 2);
                    seedCriterion(criteria, saved, "Progress, professionalism and answers", 100, 0.3, 3);
                    return saved;
                });
            }
        };
    }

    private void seedTrack(TrackRepository tracks, String code, String name) {
        if (tracks.findByCode(code).isEmpty()) {
            Track t = new Track(); t.setCode(code); t.setName(name); t.setDescription(name); tracks.save(t);
        }
    }

    private void seedRule(GradeRuleRepository repo, PhaseType phaseType, EvaluationType type, double weight) {
        if (repo.findByPhaseTypeAndEvaluationTypeAndActiveTrue(phaseType, type).isEmpty()) {
            GradeRule rule = new GradeRule(); rule.setPhaseType(phaseType); rule.setEvaluationType(type); rule.setWeight(weight); repo.save(rule);
        }
    }

    private void seedCriterion(RubricCriterionRepository repo, EvaluationFormTemplate form, String title, double maxScore, double weight, int order) {
        RubricCriterion criterion = new RubricCriterion(); criterion.setFormTemplate(form); criterion.setTitle(title); criterion.setDescription(title); criterion.setMaxScore(maxScore); criterion.setWeight(weight); criterion.setDisplayOrder(order); criterion.setRequired(true); repo.save(criterion);
    }
}
