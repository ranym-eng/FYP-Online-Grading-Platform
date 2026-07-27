package fyp_grading_platform.importing;

import fyp_grading_platform.audit.AuditService;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.user.StudentController;
import fyp_grading_platform.user.StudentProfile;
import fyp_grading_platform.user.StudentProfileRepository;
import fyp_grading_platform.user.User;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class StudentImportService {
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_UNCOMPRESSED_BYTES = 30L * 1024 * 1024;
    private static final int MAX_ZIP_ENTRIES = 2_000;
    private static final Set<String> REQUIRED_FIELDS = Set.of("studentNumber", "cohort", "fullName", "email");

    private final StudentProfileRepository students;
    private final AuditService audit;

    public StudentImportService(StudentProfileRepository students, AuditService audit) {
        this.students = students;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public StudentImportReport preview(MultipartFile file) {
        return validate(parse(file));
    }

    @Transactional
    public StudentImportReport importStudents(MultipartFile file, User actor) {
        StudentImportReport validation = validate(parse(file));
        if (!validation.importable()) return validation;

        Map<String, StudentProfile> existingById = students.findAll().stream()
                .filter(student -> student.getStudentNumber() != null)
                .collect(Collectors.toMap(StudentProfile::getStudentNumber, Function.identity(), (left, right) -> left));
        int created = 0;
        int updated = 0;
        int unchanged = 0;

        for (StudentImportRow row : validation.rows()) {
            StudentProfile profile = existingById.get(row.studentNumber());
            boolean isNew = profile == null;
            if (isNew) profile = new StudentProfile();
            boolean changed = isNew || applyOfficialFields(profile, row);
            if (changed) students.save(profile);
            if (isNew) created++;
            else if (changed) updated++;
            else unchanged++;
            existingById.put(row.studentNumber(), profile);
        }

        audit.record(
                actor.getId(),
                "STUDENTS_BULK_IMPORTED",
                "StudentProfile",
                actor.getId(),
                null,
                "file=" + safeFilename(file) + ", created=" + created + ", updated=" + updated + ", unchanged=" + unchanged
        );
        return new StudentImportReport(
                validation.sheetName(),
                validation.totalRows(),
                validation.validRows(),
                created,
                updated,
                unchanged,
                List.of(),
                validation.rows()
        );
    }

    private StudentImportReport validate(ParsedFile parsed) {
        List<StudentProfile> existing = students.findAll();
        Map<String, StudentProfile> byId = existing.stream()
                .filter(student -> student.getStudentNumber() != null)
                .collect(Collectors.toMap(StudentProfile::getStudentNumber, Function.identity(), (left, right) -> left));
        Map<String, StudentProfile> byEmail = existing.stream()
                .filter(student -> student.getEmail() != null)
                .collect(Collectors.toMap(student -> student.getEmail().toLowerCase(Locale.ROOT), Function.identity(), (left, right) -> left));
        Set<String> fileIds = new HashSet<>();
        Set<String> fileEmails = new HashSet<>();
        List<StudentImportError> errors = new ArrayList<>();
        List<StudentImportRow> rows = new ArrayList<>();

        for (RawStudentRow raw : parsed.rows()) {
            String studentNumber = StudentController.normalizeStudentNumber(raw.studentNumber());
            String fullName = StudentController.normalizeName(raw.fullName());
            String email = StudentController.normalizeEmail(raw.email());
            String cohort = normalizeCohort(raw.cohort());
            List<String> rowErrors = new ArrayList<>();

            required(raw.rowNumber(), "stdID", studentNumber, errors, rowErrors);
            required(raw.rowNumber(), "cohort", cohort, errors, rowErrors);
            required(raw.rowNumber(), "name", fullName, errors, rowErrors);
            required(raw.rowNumber(), "Email", email, errors, rowErrors);

            if (!cohort.isBlank() && !cohort.matches("(?:19|20)\\d{2}")) {
                addError(raw.rowNumber(), "cohort", raw.cohort(), "Cohort must use YY or YYYY format", errors, rowErrors);
            }
            if (!studentNumber.isBlank() && !studentNumber.matches("\\d{5,12}")) {
                addError(raw.rowNumber(), "stdID", studentNumber, "Student ID must contain 5 to 12 digits", errors, rowErrors);
            }
            if (!email.isBlank() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                addError(raw.rowNumber(), "Email", email, "Invalid email address", errors, rowErrors);
            }
            if (!studentNumber.isBlank() && !email.isBlank()) {
                String expected = "s" + studentNumber + "@student.squ.edu.om";
                if (!email.equals(expected)) {
                    addError(raw.rowNumber(), "Email", email, "Expected SQU email: " + expected, errors, rowErrors);
                }
            }
            if (!studentNumber.isBlank() && !fileIds.add(studentNumber)) {
                addError(raw.rowNumber(), "stdID", studentNumber, "Duplicate student ID in file", errors, rowErrors);
            }
            if (!email.isBlank() && !fileEmails.add(email)) {
                addError(raw.rowNumber(), "Email", email, "Duplicate email in file", errors, rowErrors);
            }
            StudentProfile emailOwner = byEmail.get(email);
            if (emailOwner != null && !emailOwner.getStudentNumber().equals(studentNumber)) {
                addError(raw.rowNumber(), "Email", email, "Email already belongs to another student", errors, rowErrors);
            }

            rows.add(new StudentImportRow(
                    raw.rowNumber(),
                    studentNumber,
                    cohort,
                    fullName,
                    email,
                    byId.containsKey(studentNumber),
                    List.copyOf(rowErrors)
            ));
        }

        int validRows = (int) rows.stream().filter(row -> row.errors().isEmpty()).count();
        return new StudentImportReport(
                parsed.sheetName(),
                rows.size(),
                validRows,
                0,
                0,
                0,
                List.copyOf(errors),
                List.copyOf(rows)
        );
    }

    private ParsedFile parse(MultipartFile file) {
        validateFile(file);
        String filename = safeFilename(file).toLowerCase(Locale.ROOT);
        try {
            if (filename.endsWith(".xlsx")) return parseWorkbook(file);
            if (filename.endsWith(".csv")) return parseCsv(file);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("INVALID_IMPORT_FILE", "The student file could not be read: " + exception.getMessage());
        }
        throw new BusinessException("UNSUPPORTED_IMPORT_FORMAT", "Supported formats are .xlsx and .csv");
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPTY_IMPORT_FILE", "Select a non-empty Excel or CSV file");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BusinessException("IMPORT_FILE_TOO_LARGE", "The import file must not exceed 10 MB");
        }
    }

    private ParsedFile parseWorkbook(MultipartFile file) throws IOException, XMLStreamException {
        Map<String, byte[]> entries = readZipEntries(file);
        byte[] workbookXml = requiredEntry(entries, "xl/workbook.xml");
        byte[] relationshipsXml = requiredEntry(entries, "xl/_rels/workbook.xml.rels");
        List<WorkbookSheet> sheets = parseWorkbookSheets(workbookXml);
        if (sheets.isEmpty()) throw new BusinessException("EMPTY_IMPORT_FILE", "The workbook contains no worksheets");
        Map<String, String> relationships = parseRelationships(relationshipsXml);
        WorkbookSheet selected = sheets.getFirst();
        String target = relationships.get(selected.relationshipId());
        if (target == null) throw new BusinessException("INVALID_IMPORT_FILE", "The first worksheet cannot be resolved");
        String sheetPath = resolveWorkbookTarget(target);
        List<String> sharedStrings = entries.containsKey("xl/sharedStrings.xml")
                ? parseSharedStrings(entries.get("xl/sharedStrings.xml"))
                : List.of();
        List<SheetRow> sheetRows = parseSheetRows(requiredEntry(entries, sheetPath), sharedStrings);
        Header header = findHeader(sheetRows);
        List<RawStudentRow> rows = new ArrayList<>();

        for (SheetRow row : sheetRows) {
            if (row.rowNumber() <= header.rowNumber()) continue;
            Map<String, String> values = new HashMap<>();
            header.columns().forEach((field, column) -> values.put(field, valueAt(row.values(), column)));
            if (values.values().stream().allMatch(String::isBlank)) continue;
            rows.add(new RawStudentRow(
                    row.rowNumber(),
                    values.getOrDefault("studentNumber", ""),
                    values.getOrDefault("cohort", ""),
                    values.getOrDefault("fullName", ""),
                    values.getOrDefault("email", "")
            ));
        }
        return new ParsedFile(selected.name(), rows);
    }

    private Map<String, byte[]> readZipEntries(MultipartFile file) throws IOException {
        Map<String, byte[]> entries = new HashMap<>();
        long totalBytes = 0;
        int entryCount = 0;
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                if (++entryCount > MAX_ZIP_ENTRIES) {
                    throw new BusinessException("INVALID_IMPORT_FILE", "The workbook contains too many ZIP entries");
                }
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = zip.read(buffer)) >= 0) {
                    output.write(buffer, 0, read);
                    totalBytes += read;
                    if (totalBytes > MAX_UNCOMPRESSED_BYTES) {
                        throw new BusinessException("INVALID_IMPORT_FILE", "The expanded workbook is too large");
                    }
                }
                entries.put(entry.getName().replace('\\', '/'), output.toByteArray());
            }
        }
        return entries;
    }

    private List<WorkbookSheet> parseWorkbookSheets(byte[] xml) throws XMLStreamException {
        List<WorkbookSheet> sheets = new ArrayList<>();
        XMLStreamReader reader = xmlReader(xml);
        while (reader.hasNext()) {
            if (reader.next() == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("sheet")) {
                sheets.add(new WorkbookSheet(attribute(reader, "name"), attribute(reader, "id")));
            }
        }
        reader.close();
        return sheets;
    }

    private Map<String, String> parseRelationships(byte[] xml) throws XMLStreamException {
        Map<String, String> relationships = new HashMap<>();
        XMLStreamReader reader = xmlReader(xml);
        while (reader.hasNext()) {
            if (reader.next() == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("Relationship")) {
                relationships.put(attribute(reader, "Id"), attribute(reader, "Target"));
            }
        }
        reader.close();
        return relationships;
    }

    private List<String> parseSharedStrings(byte[] xml) throws XMLStreamException {
        List<String> values = new ArrayList<>();
        XMLStreamReader reader = xmlReader(xml);
        StringBuilder current = null;
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("si")) {
                current = new StringBuilder();
            } else if (event == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("t") && current != null) {
                current.append(reader.getElementText());
            } else if (event == XMLStreamConstants.END_ELEMENT && reader.getLocalName().equals("si") && current != null) {
                values.add(current.toString());
                current = null;
            }
        }
        reader.close();
        return values;
    }

    private List<SheetRow> parseSheetRows(byte[] xml, List<String> sharedStrings) throws XMLStreamException {
        List<SheetRow> rows = new ArrayList<>();
        XMLStreamReader reader = xmlReader(xml);
        Map<Integer, String> currentRow = null;
        int rowNumber = 0;
        int column = -1;
        String cellType = null;
        String cellValue = "";
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("row")) {
                currentRow = new HashMap<>();
                rowNumber = parseInteger(attribute(reader, "r"), rows.size() + 1);
            } else if (event == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("c")) {
                column = columnIndex(attribute(reader, "r"));
                cellType = attribute(reader, "t");
                cellValue = "";
            } else if (event == XMLStreamConstants.START_ELEMENT
                    && (reader.getLocalName().equals("v") || reader.getLocalName().equals("t"))
                    && currentRow != null && column >= 0) {
                cellValue = reader.getElementText();
            } else if (event == XMLStreamConstants.END_ELEMENT && reader.getLocalName().equals("c") && currentRow != null) {
                currentRow.put(column, decodeCell(cellType, cellValue, sharedStrings));
                column = -1;
            } else if (event == XMLStreamConstants.END_ELEMENT && reader.getLocalName().equals("row") && currentRow != null) {
                int maxColumn = currentRow.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);
                List<String> values = new ArrayList<>();
                for (int index = 0; index <= maxColumn; index++) values.add(currentRow.getOrDefault(index, ""));
                rows.add(new SheetRow(rowNumber, values));
                currentRow = null;
            }
        }
        reader.close();
        return rows;
    }

    private ParsedFile parseCsv(MultipartFile file) throws IOException {
        String content;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            content = reader.lines().collect(Collectors.joining("\n"));
        }
        char delimiter = content.lines().findFirst().orElse("").chars().filter(value -> value == ';').count() > 0 ? ';' : ',';
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get();
        List<RawStudentRow> rows = new ArrayList<>();
        try (CSVParser parser = CSVParser.parse(content, format)) {
            Map<String, String> canonicalHeaders = new LinkedHashMap<>();
            parser.getHeaderNames().forEach(header -> {
                String canonical = canonicalHeader(header);
                if (canonical != null) canonicalHeaders.put(canonical, header);
            });
            assertRequiredHeaders(canonicalHeaders.keySet());
            for (CSVRecord record : parser) {
                String studentNumber = record.get(canonicalHeaders.get("studentNumber")).trim();
                String cohort = record.get(canonicalHeaders.get("cohort")).trim();
                String fullName = record.get(canonicalHeaders.get("fullName")).trim();
                String email = record.get(canonicalHeaders.get("email")).trim();
                if (studentNumber.isBlank() && cohort.isBlank() && fullName.isBlank() && email.isBlank()) continue;
                rows.add(new RawStudentRow((int) record.getRecordNumber() + 1, studentNumber, cohort, fullName, email));
            }
        }
        return new ParsedFile(safeFilename(file), rows);
    }

    private Header findHeader(List<SheetRow> rows) {
        for (SheetRow row : rows.stream().limit(25).toList()) {
            Map<String, Integer> columns = new LinkedHashMap<>();
            for (int column = 0; column < row.values().size(); column++) {
                String canonical = canonicalHeader(row.values().get(column));
                if (canonical != null) columns.putIfAbsent(canonical, column);
            }
            if (columns.keySet().containsAll(REQUIRED_FIELDS)) return new Header(row.rowNumber(), columns);
        }
        throw new BusinessException("INVALID_STUDENT_IMPORT_HEADERS", "Expected columns: stdID, cohort, name, Email");
    }

    private void assertRequiredHeaders(Set<String> fields) {
        if (!fields.containsAll(REQUIRED_FIELDS)) {
            throw new BusinessException("INVALID_STUDENT_IMPORT_HEADERS", "Expected columns: stdID, cohort, name, Email");
        }
    }

    private String canonicalHeader(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
        return switch (normalized) {
            case "stdid", "studentid", "studentnumber", "identifiantetudiant", "idetudiant", "id" -> "studentNumber";
            case "cohort", "promotion", "cohorte" -> "cohort";
            case "name", "fullname", "nomcomplet", "nom" -> "fullName";
            case "email", "adressemail", "courriel" -> "email";
            default -> null;
        };
    }

    private XMLStreamReader xmlReader(byte[] xml) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
        return factory.createXMLStreamReader(new ByteArrayInputStream(xml));
    }

    private String resolveWorkbookTarget(String target) {
        String normalized = target.replace('\\', '/');
        if (normalized.startsWith("/")) return normalized.substring(1);
        return URI.create("xl/").resolve(normalized).normalize().getPath();
    }

    private byte[] requiredEntry(Map<String, byte[]> entries, String path) {
        byte[] value = entries.get(path);
        if (value == null) throw new BusinessException("INVALID_IMPORT_FILE", "Missing Excel entry: " + path);
        return value;
    }

    private String attribute(XMLStreamReader reader, String localName) {
        for (int index = 0; index < reader.getAttributeCount(); index++) {
            if (reader.getAttributeLocalName(index).equals(localName)) return reader.getAttributeValue(index);
        }
        return "";
    }

    private int columnIndex(String reference) {
        int result = 0;
        int index = 0;
        while (index < reference.length() && Character.isLetter(reference.charAt(index))) {
            result = result * 26 + Character.toUpperCase(reference.charAt(index)) - 'A' + 1;
            index++;
        }
        return Math.max(0, result - 1);
    }

    private String decodeCell(String type, String value, List<String> sharedStrings) {
        if ("s".equals(type)) {
            int index = parseInteger(value, -1);
            return index >= 0 && index < sharedStrings.size() ? sharedStrings.get(index).trim() : "";
        }
        return value == null ? "" : value.trim().replaceFirst("\\.0$", "");
    }

    private int parseInteger(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private String valueAt(List<String> values, int index) {
        return index >= 0 && index < values.size() ? values.get(index).trim() : "";
    }

    private String normalizeCohort(String value) {
        if (value == null || value.isBlank()) return "";
        String cohort = value.trim().replaceFirst("\\.0$", "");
        return cohort.matches("\\d{2}") ? "20" + cohort : cohort;
    }

    private void required(
            int rowNumber,
            String field,
            String value,
            List<StudentImportError> errors,
            List<String> rowErrors
    ) {
        if (value != null && !value.isBlank()) return;
        addError(rowNumber, field, value, "Required value is missing", errors, rowErrors);
    }

    private void addError(
            int rowNumber,
            String field,
            String value,
            String message,
            List<StudentImportError> errors,
            List<String> rowErrors
    ) {
        errors.add(new StudentImportError(rowNumber, field, value, message));
        rowErrors.add(message);
    }

    private boolean applyOfficialFields(StudentProfile profile, StudentImportRow row) {
        boolean changed = !row.studentNumber().equals(profile.getStudentNumber())
                || !row.fullName().equals(profile.getFullName())
                || !row.email().equals(profile.getEmail())
                || !row.cohort().equals(profile.getCohort());
        profile.setStudentNumber(row.studentNumber());
        profile.setFullName(row.fullName());
        profile.setEmail(row.email());
        profile.setCohort(row.cohort());
        return changed;
    }

    private String safeFilename(MultipartFile file) {
        String original = file.getOriginalFilename();
        return original == null || original.isBlank() ? "students.xlsx" : original.replaceAll("[\\r\\n]", "");
    }

    private record Header(int rowNumber, Map<String, Integer> columns) {}
    private record ParsedFile(String sheetName, List<RawStudentRow> rows) {}
    private record RawStudentRow(int rowNumber, String studentNumber, String cohort, String fullName, String email) {}
    private record WorkbookSheet(String name, String relationshipId) {}
    private record SheetRow(int rowNumber, List<String> values) {}
}