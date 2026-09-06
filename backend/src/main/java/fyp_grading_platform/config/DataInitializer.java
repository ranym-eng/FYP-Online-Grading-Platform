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
import org.springframework.beans.factory.annotation.Value;
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
            RubricCriterionRepository criteria,
            @Value("${app.bootstrap.admin-enabled:false}") boolean bootstrapAdminEnabled,
            @Value("${app.bootstrap.admin-email:admin@squ.edu.om}") String bootstrapAdminEmail,
            @Value("${app.bootstrap.admin-name:FYP Administrator}") String bootstrapAdminName,
            @Value("${app.bootstrap.admin-university-id:ADMIN-001}") String bootstrapAdminUniversityId,
            @Value("${app.bootstrap.admin-password:}") String bootstrapAdminPassword
    ) {
        return args -> {
            seedTrack(tracks, "EIC", "Electronics, Instrumentation and Control");
            seedTrack(tracks, "CSN", "Communication and Signal Networks");
            seedTrack(tracks, "CSP", "Computer Systems and Programming");
            seedTrack(tracks, "PSE", "Power Systems Engineering");

            seedBootstrapAdmin(
                    users,
                    encoder,
                    bootstrapAdminEnabled,
                    bootstrapAdminEmail,
                    bootstrapAdminName,
                    bootstrapAdminUniversityId,
                    bootstrapAdminPassword
            );

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
                } else if (type == EvaluationType.REPORT_PHASE_I || type == EvaluationType.REPORT_PHASE_II) {
                    ensureReportForm(type, forms, criteria);
                } else {
                    ensureDefaultForm(type, forms, criteria);
                }
            }
        };
    }

    void seedBootstrapAdmin(
            UserRepository users,
            PasswordEncoder encoder,
            boolean enabled,
            String email,
            String fullName,
            String universityId,
            String password
    ) {
        if (!enabled) return;

        String normalizedEmail = requiredSetting("APP_BOOTSTRAP_ADMIN_EMAIL", email).toLowerCase();
        String normalizedName = requiredSetting("APP_BOOTSTRAP_ADMIN_NAME", fullName);
        String normalizedUniversityId = requiredSetting("APP_BOOTSTRAP_ADMIN_UNIVERSITY_ID", universityId);
        String initialPassword = requiredSetting("APP_BOOTSTRAP_ADMIN_PASSWORD", password);

        User existingAdmin = users.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (existingAdmin != null) {
            if (!normalizedUniversityId.equals(existingAdmin.getUniversityId())
                    && users.existsByUniversityId(normalizedUniversityId)) {
                throw duplicateUniversityId(normalizedUniversityId);
            }

            boolean changed = false;
            if (!normalizedName.equals(existingAdmin.getFullName())) {
                existingAdmin.setFullName(normalizedName);
                changed = true;
            }
            if (!normalizedUniversityId.equals(existingAdmin.getUniversityId())) {
                existingAdmin.setUniversityId(normalizedUniversityId);
                changed = true;
            }
            if (existingAdmin.getRole() != UserRole.ADMIN) {
                existingAdmin.setRole(UserRole.ADMIN);
                changed = true;
            }
            if (existingAdmin.getStatus() != UserStatus.ACTIVE) {
                existingAdmin.setStatus(UserStatus.ACTIVE);
                changed = true;
            }
            if (existingAdmin.getPasswordHash() == null || existingAdmin.getPasswordHash().isBlank()) {
                existingAdmin.setPasswordHash(encoder.encode(initialPassword));
                existingAdmin.setPasswordChangeRequired(false);
                existingAdmin.setTemporaryPasswordExpiresAt(null);
                changed = true;
            }
            if (changed) users.save(existingAdmin);
            return;
        }

        if (users.existsByUniversityId(normalizedUniversityId)) {
            throw duplicateUniversityId(normalizedUniversityId);
        }

        User admin = new User();
        admin.setUniversityId(normalizedUniversityId);
        admin.setFullName(normalizedName);
        admin.setEmail(normalizedEmail);
        admin.setPasswordHash(encoder.encode(initialPassword));
        admin.setPasswordChangeRequired(false);
        admin.setTemporaryPasswordExpiresAt(null);
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        users.save(admin);
    }

    private IllegalStateException duplicateUniversityId(String universityId) {
        return new IllegalStateException(
                "Bootstrap administrator university ID already belongs to another account: " + universityId
        );
    }

    private String requiredSetting(String environmentName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(environmentName + " is required when bootstrap administration is enabled");
        }
        return value.trim();
    }

    private void ensureReportForm(
            EvaluationType type,
            EvaluationFormTemplateRepository forms,
            RubricCriterionRepository criteria
    ) {
        List<EvaluationFormTemplate> existingForms = forms.findByEvaluationType(type);
        EvaluationFormTemplate officialForm = existingForms.stream()
                .filter(EvaluationFormTemplate::isActive)
                .filter(form -> isOfficialReportRubric(
                        criteria.findByFormTemplateIdOrderByDisplayOrderAsc(form.getId())))
                .findFirst()
                .orElse(null);

        if (officialForm != null) {
            existingForms.stream()
                    .filter(EvaluationFormTemplate::isActive)
                    .filter(form -> !form.getId().equals(officialForm.getId()))
                    .forEach(form -> {
                        form.setActive(false);
                        forms.save(form);
                    });
            officialForm.setName(type == EvaluationType.REPORT_PHASE_I
                    ? "FYP I Paper Report Evaluation"
                    : "FYP II Paper Report Evaluation");
            officialForm.setDescription("Official paper report rubric. Ten criteria are scored out of 10; criterion 4 counts twice.");
            officialForm.setTotalWeight(11);
            forms.save(officialForm);
            return;
        }

        existingForms.stream()
                .filter(EvaluationFormTemplate::isActive)
                .forEach(form -> {
                    form.setActive(false);
                    forms.save(form);
                });

        EvaluationFormTemplate form = new EvaluationFormTemplate();
        form.setName(type == EvaluationType.REPORT_PHASE_I
                ? "FYP I Paper Report Evaluation"
                : "FYP II Paper Report Evaluation");
        form.setEvaluationType(type);
        form.setPhaseType(type == EvaluationType.REPORT_PHASE_I ? PhaseType.PHASE_I : PhaseType.PHASE_II);
        form.setDescription("Official paper report rubric. Ten criteria are scored out of 10; criterion 4 counts twice.");
        form.setTotalWeight(11);
        form = forms.save(form);

        seedCriterion(criteria, form, "Identify and state a complex engineering problem (1.a)", 10, 1, 1);
        seedCriterion(criteria, form, "Formulate the complex engineering problem using diagrams, equations or flowcharts (1.b)", 10, 1, 2);
        seedCriterion(criteria, form, "Specify the design requirements and constraints of the complex engineering problem (2.a)", 10, 1, 3);
        seedCriterion(criteria, form, "Analyze and produce solutions using alternatives, mathematical formulation, simulations or implementations (1.c)", 10, 2, 4);
        seedCriterion(criteria, form, "Develop and evaluate possible solutions under realistic constraints and engineering standards (2.b)", 10, 1, 5);
        seedCriterion(criteria, form, "Select components, build and test the design, and produce the solution meeting the requirements (2.c)", 10, 1, 6);
        seedCriterion(criteria, form, "Write a technical report with proper formatting and English (3.a)", 10, 1, 7);
        seedCriterion(criteria, form, "Demonstrate understanding of professional ethics, citations and similarity requirements (4.a)", 10, 1, 8);
        seedCriterion(criteria, form, "Evaluate professional ethics and global, economic, environmental and societal impacts (4.c)", 10, 1, 9);
        seedCriterion(criteria, form, "Complete the proposed work", 10, 1, 10);
    }

    private boolean isOfficialReportRubric(List<RubricCriterion> criteria) {
        if (criteria.size() != 10) return false;
        double[] weights = {1, 1, 1, 2, 1, 1, 1, 1, 1, 1};
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
