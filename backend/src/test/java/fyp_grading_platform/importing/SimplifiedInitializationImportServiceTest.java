package fyp_grading_platform.importing;

import fyp_grading_platform.audit.AuditService;
import fyp_grading_platform.auth.IndustryInvitationService;
import fyp_grading_platform.auth.OneTimeTokenHasher;
import fyp_grading_platform.project.ProjectEvaluatorAssignmentRepository;
import fyp_grading_platform.project.ProjectRepository;
import fyp_grading_platform.project.ProjectSupervisorAssignmentRepository;
import fyp_grading_platform.project.TeamRepository;
import fyp_grading_platform.project.Track;
import fyp_grading_platform.project.TrackRepository;
import fyp_grading_platform.user.EvaluatorProfileRepository;
import fyp_grading_platform.user.StudentProfileRepository;
import fyp_grading_platform.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimplifiedInitializationImportServiceTest {
    @Mock StudentProfileRepository students;
    @Mock UserRepository users;
    @Mock EvaluatorProfileRepository evaluatorProfiles;
    @Mock TrackRepository tracks;
    @Mock ProjectRepository projects;
    @Mock TeamRepository teams;
    @Mock ProjectSupervisorAssignmentRepository supervisors;
    @Mock ProjectEvaluatorAssignmentRepository evaluators;
    @Mock PasswordEncoder passwordEncoder;
    @Mock OneTimeTokenHasher tokenHasher;
    @Mock IndustryInvitationService industryInvitations;
    @Mock AuditService audit;

    @Test
    void officialDownloadableTemplatePassesPreviewValidation() throws Exception {
        List<Track> configuredTracks = List.of(
                track("EIC"), track("PSE"), track("CSP"), track("CSN")
        );
        when(tracks.findAll()).thenReturn(configuredTracks);
        when(tracks.findByCode(anyString())).thenAnswer(invocation -> configuredTracks.stream()
                .filter(track -> track.getCode().equals(invocation.getArgument(0)))
                .findFirst());
        Path template = Path.of("../frontend/public/modele_initialisation_plateforme_fyp.xlsx");
        MockMultipartFile workbook = new MockMultipartFile(
                "file",
                template.getFileName().toString(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                Files.readAllBytes(template)
        );

        InitializationImportReport report = service().preview(workbook);

        assertTrue(report.importable(), () -> "Template errors: " + report.errors());
        assertEquals(7, report.sheets().size());
        assertEquals(32, report.totalRows());
        assertEquals(32, report.validRows());
    }

    private SimplifiedInitializationImportService service() {
        return new SimplifiedInitializationImportService(
                students,
                users,
                evaluatorProfiles,
                tracks,
                projects,
                teams,
                supervisors,
                evaluators,
                passwordEncoder,
                tokenHasher,
                industryInvitations,
                audit
        );
    }

    private Track track(String code) {
        Track track = new Track();
        track.setCode(code);
        track.setName(code);
        return track;
    }
}
