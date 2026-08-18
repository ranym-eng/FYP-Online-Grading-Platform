package fyp_grading_platform.importing;

import fyp_grading_platform.audit.AuditService;
import fyp_grading_platform.user.StudentProfile;
import fyp_grading_platform.user.StudentProfileRepository;
import fyp_grading_platform.user.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentImportServiceTest {
    private final StudentProfileRepository students = mock(StudentProfileRepository.class);
    private final StudentImportService service = new StudentImportService(students, mock(AuditService.class));

    @Test
    void parsesOfficialHeadersAndNormalizesTwoDigitCohort() throws Exception {
        when(students.findAll()).thenReturn(List.of());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookWithOneStudent()
        );

        StudentImportReport report = service.preview(file);

        assertTrue(report.importable());
        assertEquals(1, report.totalRows());
        assertEquals("142430", report.rows().getFirst().studentNumber());
        assertEquals("2022", report.rows().getFirst().cohort());
        assertEquals("Mohammed Qasim Al Saadi", report.rows().getFirst().fullName());
        assertEquals("s142430@student.squ.edu.om", report.rows().getFirst().email());
    }

    @Test
    void populatesRequiredFieldsBeforeSavingANewStudent() throws Exception {
        when(students.findAll()).thenReturn(List.of());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookWithOneStudent()
        );
        User actor = new User();
        actor.setId(UUID.randomUUID());

        StudentImportReport report = service.importStudents(file, actor);

        ArgumentCaptor<StudentProfile> savedStudent = ArgumentCaptor.forClass(StudentProfile.class);
        verify(students).save(savedStudent.capture());
        assertEquals(1, report.created());
        assertEquals("142430", savedStudent.getValue().getStudentNumber());
        assertEquals("2022", savedStudent.getValue().getCohort());
        assertEquals("Mohammed Qasim Al Saadi", savedStudent.getValue().getFullName());
        assertEquals("s142430@student.squ.edu.om", savedStudent.getValue().getEmail());
    }

    @Test
    void parsesConfiguredUniversitySample() throws Exception {
        String source = System.getProperty("student.import.sample", "");
        assumeTrue(!source.isBlank() && Files.exists(Path.of(source)));
        when(students.findAll()).thenReturn(List.of());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                Path.of(source).getFileName().toString(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                Files.readAllBytes(Path.of(source))
        );

        StudentImportReport report = service.preview(file);

        assertEquals(130, report.totalRows());
        assertEquals(130, report.validRows());
        assertTrue(report.errors().isEmpty());
    }

    private byte[] workbookWithOneStudent() throws Exception {
        String workbook = """
                <?xml version="1.0" encoding="UTF-8"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets><sheet name="Sheet1" sheetId="1" r:id="rId1"/></sheets>
                </workbook>
                """;
        String relationships = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Target="worksheets/sheet1.xml"
                                Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"/>
                </Relationships>
                """;
        String sheet = """
                <?xml version="1.0" encoding="UTF-8"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                    <row r="1">
                      <c r="A1" t="inlineStr"><is><t>stdID</t></is></c>
                      <c r="B1" t="inlineStr"><is><t>cohort</t></is></c>
                      <c r="C1" t="inlineStr"><is><t>name</t></is></c>
                      <c r="D1" t="inlineStr"><is><t>Email</t></is></c>
                    </row>
                    <row r="2">
                      <c r="A2"><v>142430</v></c>
                      <c r="B2"><v>22</v></c>
                      <c r="C2" t="inlineStr"><is><t>Mohammed Qasim Al Saadi</t></is></c>
                      <c r="D2" t="inlineStr"><is><t>s142430@student.squ.edu.om</t></is></c>
                    </row>
                  </sheetData>
                </worksheet>
                """;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            write(zip, "xl/workbook.xml", workbook);
            write(zip, "xl/_rels/workbook.xml.rels", relationships);
            write(zip, "xl/worksheets/sheet1.xml", sheet);
        }
        return output.toByteArray();
    }

    private void write(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}