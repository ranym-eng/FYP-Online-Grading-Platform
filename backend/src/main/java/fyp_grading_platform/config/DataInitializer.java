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

import java.util.List;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedData(
            TrackRepository tracks,
            UserRepository users,
            PasswordEncoder encoder,
            GradeRuleRepository gradeRules,
            EvaluationFormTemplateRepository forms,
            RubricCriterionRepository criteria
    ) {
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
                if (type == EvaluationType.DEMO_DAY_INDUSTRY) {
                    ensureIndustryGuestForm(forms, criteria);
                } else {
                    ensureDefaultForm(type, forms, criteria);
                }
            }
        };
    }

    private void ensureDefaultForm(
            EvaluationType type,
            EvaluationFormTemplateRepository forms,
            RubricCriterionRepository criteria
    ) {
        EvaluationFormTemplate form = forms.findFirstByEvaluationTypeAndActiveTrue(type).orElseGet(() -> {
            EvaluationFormTemplate created = new EvaluationFormTemplate();
            created.setName(type.name().replace('_', ' '));
            created.setEvaluationType(type);
            created.setPhaseType(type.name().endsWith("PHASE_I") ? PhaseType.PHASE_I : PhaseType.PHASE_II);
            created.setDescription("Default rubric generated from project specification");
            created.setTotalWeight(100);
            return forms.save(created);
        });
        if (criteria.findByFormTemplateIdOrderByDisplayOrderAsc(form.getId()).isEmpty()) {
            seedCriterion(criteria, form, "Technical quality", 100, 0.4, 1);
            seedCriterion(criteria, form, "Report / presentation clarity", 100, 0.3, 2);
            seedCriterion(criteria, form, "Progress, professionalism and answers", 100, 0.3, 3);
        }
    }

    private void ensureIndustryGuestForm(
            EvaluationFormTemplateRepository forms,
            RubricCriterionRepository criteria
    ) {
        EvaluationFormTemplate current = forms.findFirstByEvaluationTypeAndActiveTrue(EvaluationType.DEMO_DAY_INDUSTRY)
                .orElse(null);
        if (current != null && isOfficialIndustryGuestRubric(criteria.findByFormTemplateIdOrderByDisplayOrderAsc(current.getId()))) {
            current.setName("FYP Demo Evaluation - Industry Guest");
            current.setDescription("Per-project demo/prototype evaluation. Each component is scored out of 10.");
            current.setTotalWeight(10);
            forms.save(current);
            return;
        }
        if (current != null) {
            current.setActive(false);
            forms.save(current);
        }

        EvaluationFormTemplate form = new EvaluationFormTemplate();
        form.setName("FYP Demo Evaluation - Industry Guest");
        form.setEvaluationType(EvaluationType.DEMO_DAY_INDUSTRY);
        form.setPhaseType(PhaseType.PHASE_II);
        form.setDescription("Per-project demo/prototype evaluation. Each component is scored out of 10.");
        form.setTotalWeight(10);
        form = forms.save(form);

        seedCriterion(criteria, form, "Select components, build and test the project prototype", 10, 2, 1);
        seedCriterion(criteria, form, "Present the project prototype in a clear and logical sequence", 10, 1, 2);
        seedCriterion(criteria, form, "Respond to questions and comments effectively", 10, 4, 3);
        seedCriterion(criteria, form, "Complete the proposed work", 10, 2, 4);
        seedCriterion(criteria, form, "Produce a poster: design, technical content and English", 10, 1, 5);
    }

    private boolean isOfficialIndustryGuestRubric(List<RubricCriterion> criteria) {
        if (criteria.size() != 5) return false;
        double[] weights = {2, 1, 4, 2, 1};
        for (int index = 0; index < criteria.size(); index++) {
            RubricCriterion criterion = criteria.get(index);
            if (criterion.getDisplayOrder() != index + 1
                    || Double.compare(criterion.getMaxScore(), 10) != 0
                    || Double.compare(criterion.getWeight(), weights[index]) != 0) {
                return false;
            }
        }
        return true;
    }

    private void seedTrack(TrackRepository tracks, String code, String name) {
        if (tracks.findByCode(code).isEmpty()) {
            Track track = new Track();
            track.setCode(code);
            track.setName(name);
            track.setDescription(name);
            tracks.save(track);
        }
    }

    private void seedRule(
            GradeRuleRepository repository,
            PhaseType phaseType,
            EvaluationType type,
            double weight
    ) {
        if (repository.findByPhaseTypeAndEvaluationTypeAndActiveTrue(phaseType, type).isEmpty()) {
            GradeRule rule = new GradeRule();
            rule.setPhaseType(phaseType);
            rule.setEvaluationType(type);
            rule.setWeight(weight);
            repository.save(rule);
        }
    }

    private void seedCriterion(
            RubricCriterionRepository repository,
            EvaluationFormTemplate form,
            String title,
            double maxScore,
            double weight,
            int order
    ) {
        RubricCriterion criterion = new RubricCriterion();
        criterion.setFormTemplate(form);
        criterion.setTitle(title);
        criterion.setDescription(title);
        criterion.setMaxScore(maxScore);
        criterion.setWeight(weight);
        criterion.setDisplayOrder(order);
        criterion.setRequired(true);
        repository.save(criterion);
    }
}