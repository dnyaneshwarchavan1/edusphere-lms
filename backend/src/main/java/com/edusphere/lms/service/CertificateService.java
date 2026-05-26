package com.edusphere.lms.service;

import com.edusphere.lms.dto.FeatureDtos.CertificateResponse;
import com.edusphere.lms.entity.Certificate;
import com.edusphere.lms.entity.Course;
import com.edusphere.lms.entity.Enrollment;
import com.edusphere.lms.entity.User;
import com.edusphere.lms.exception.ApiException;
import com.edusphere.lms.repository.CertificateRepository;
import com.edusphere.lms.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CertificateService {
    private final EnrollmentRepository enrollmentRepository;
    private final CourseService courseService;
    private final CertificateRepository certificateRepository;

    @Transactional(readOnly = true)
    public List<CertificateResponse> myCertificates(User student) {
        return certificateRepository.findByStudentOrderByIssuedAtDesc(student).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CertificateResponse generate(Long courseId, String recipientName, User student) {
        Enrollment enrollment = requireCompletedEnrollment(courseId, student);
        return toResponse(ensureCertificate(enrollment, recipientName));
    }

    @Transactional(readOnly = true)
    public CertificateResponse get(Long courseId, User student) {
        Enrollment enrollment = requireCompletedEnrollment(courseId, student);
        Certificate certificate = certificateRepository.findByStudentAndCourse(enrollment.getStudent(), enrollment.getCourse())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Generate the certificate before downloading it"));
        return toResponse(certificate);
    }

    @Transactional
    public byte[] download(Long courseId, User student) {
        Enrollment enrollment = requireCompletedEnrollment(courseId, student);
        Certificate certificate = certificateRepository.findByStudentAndCourse(enrollment.getStudent(), enrollment.getCourse())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Generate the certificate before downloading it"));
        try {
            return buildPdf(certificate, enrollment);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate certificate PDF right now");
        }
    }

    @Transactional
    public void ensureCertificateIfEligible(Enrollment enrollment) {
        if (enrollment.getProgressPercent() >= 100 && certificateRepository.findByStudentAndCourse(enrollment.getStudent(), enrollment.getCourse()).isPresent()) {
            return;
        }
    }

    private Enrollment requireCompletedEnrollment(Long courseId, User student) {
        Course course = courseService.findCourse(courseId);
        Enrollment enrollment = enrollmentRepository.findByStudentAndCourse(student, course)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Enrollment not found for this course"));
        if (enrollment.getProgressPercent() < 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Complete the course before downloading the certificate");
        }
        return enrollment;
    }

    private Certificate ensureCertificate(Enrollment enrollment, String recipientName) {
        String safeName = recipientName == null ? "" : recipientName.trim().replaceAll("\\s+", " ");
        if (safeName.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Student full name is required for certificate generation");
        }

        Certificate certificate = certificateRepository.findByStudentAndCourse(enrollment.getStudent(), enrollment.getCourse())
                .orElseGet(() -> Certificate.builder()
                        .student(enrollment.getStudent())
                        .course(enrollment.getCourse())
                        .certificateNumber(buildCertificateCode(enrollment))
                        .build());
        certificate.setRecipientName(safeName);
        return certificateRepository.save(certificate);
    }

    private CertificateResponse toResponse(Certificate certificate) {
        Enrollment enrollment = enrollmentRepository.findByStudentAndCourse(certificate.getStudent(), certificate.getCourse())
                .orElse(null);
        return new CertificateResponse(
                certificate.getId(),
                certificate.getCourse().getId(),
                certificate.getCourse().getTitle(),
                certificate.getRecipientName() == null || certificate.getRecipientName().isBlank()
                        ? certificate.getStudent().getName()
                        : certificate.getRecipientName(),
                certificate.getCertificateNumber(),
                enrollment == null ? 100 : enrollment.getProgressPercent(),
                certificate.getIssuedAt()
        );
    }

    private byte[] buildPdf(Certificate certificate, Enrollment enrollment) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
            document.addPage(page);
            float width = page.getMediaBox().getWidth();
            float height = page.getMediaBox().getHeight();

            PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD_ITALIC);
            PDType1Font brandFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font bodyFont = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
            PDType1Font nameFont = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD_ITALIC);
            PDType1Font courseFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font footerFont = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                setFillColor(content, 250, 250, 252);
                content.addRect(0, 0, width, height);
                content.fill();

                drawBorder(content, 12, 12, width - 24, height - 24, 42, 51, 68, 4f);
                drawBorder(content, 28, 28, width - 56, height - 56, 59, 130, 246, 1.7f);
                drawBorder(content, 42, 42, width - 84, height - 84, 180, 188, 205, 0.8f);

                drawImageIfPresent(document, content, "certificates/edusphere-logo.png", 105, height - 165, 92, 112);
                writeLeft(content, brandFont, 24, "EduSphere", 215, height - 84);
                writeLeft(content, footerFont, 12, "Learning System", 216, height - 106);
                writeCenteredInRegion(content, titleFont, 34, "Certificate of Completion", 250, width - 110, height - 100);

                writeCentered(content, brandFont, 15, "This document verifies that", width, height - 192);
                writeFittedCentered(content, nameFont, 30, 18, certificate.getRecipientName(), 120, width - 120, height - 272);
                writeCentered(content, bodyFont, 15, "has successfully completed all course requirements for", width, height - 346);
                writeFittedCentered(content, courseFont, 22, 14, certificate.getCourse().getTitle(), 100, width - 100, height - 408);
                writeCentered(content, footerFont, 16, "Issued by EduSphere Learning System", width, height - 460);
                writeCentered(content, footerFont, 15, "with " + enrollment.getProgressPercent() + "% completion and verified learning progress.", width, height - 496);

                String issueDate = formatDate(certificate.getIssuedAt(), "dd/MM/yyyy");
                writeLeft(content, footerFont, 14, "Date:", 78, 126);
                writeLeft(content, footerFont, 14, issueDate, 78, 98);

                writeCentered(content, footerFont, 16, "Issued on " + formatDate(certificate.getIssuedAt(), "dd MMMM yyyy"), width, 126);
                writeCentered(content, footerFont, 15, certificate.getCourse().getInstructor().getName(), width, 98);
                writeCentered(content, footerFont, 14, "Course Coordinator", width, 72);
                writeCentered(content, footerFont, 14, "Certificate No: " + certificate.getCertificateNumber(), width, 48);

                float signatureBoxX = width - 204;
                float signatureBoxY = 56;
                float signatureBoxW = 160;
                float signatureBoxH = 104;
                drawBorder(content, signatureBoxX, signatureBoxY, signatureBoxW, signatureBoxH, 210, 214, 220, 0.8f);
                drawImageIfPresent(document, content, "certificates/signature.png", signatureBoxX + 12, signatureBoxY + 26, 120, 44);
                drawImageIfPresent(document, content, "certificates/naac-badge.png", signatureBoxX + 102, signatureBoxY + 16, 48, 38);
            }

            document.save(output);
            return output.toByteArray();
        }
    }

    private void writeCentered(PDPageContentStream content, PDType1Font font, float fontSize, String text, float pageWidth, float y) throws IOException {
        String safeText = safePdfText(text);
        float textWidth = font.getStringWidth(safeText) / 1000f * fontSize;
        content.beginText();
        content.setFont(font, fontSize);
        setFillColor(content, 23, 33, 38);
        content.newLineAtOffset((pageWidth - textWidth) / 2, y);
        content.showText(safeText);
        content.endText();
    }

    private void writeLeft(PDPageContentStream content, PDType1Font font, float fontSize, String text, float x, float y) throws IOException {
        String safeText = safePdfText(text);
        content.beginText();
        content.setFont(font, fontSize);
        setFillColor(content, 23, 33, 38);
        content.newLineAtOffset(x, y);
        content.showText(safeText);
        content.endText();
    }

    private void writeCenteredInRegion(PDPageContentStream content, PDType1Font font, float fontSize, String text, float left, float right, float y) throws IOException {
        String safeText = safePdfText(text);
        float textWidth = font.getStringWidth(safeText) / 1000f * fontSize;
        float x = left + ((right - left) - textWidth) / 2f;
        content.beginText();
        content.setFont(font, fontSize);
        setFillColor(content, 23, 33, 38);
        content.newLineAtOffset(x, y);
        content.showText(safeText);
        content.endText();
    }

    private void writeFittedCentered(PDPageContentStream content, PDType1Font font, float preferredSize, float minimumSize, String text, float left, float right, float y) throws IOException {
        String safeText = safePdfText(text);
        float fontSize = preferredSize;
        float availableWidth = right - left;
        while (fontSize > minimumSize) {
            float textWidth = font.getStringWidth(safeText) / 1000f * fontSize;
            if (textWidth <= availableWidth) {
                break;
            }
            fontSize -= 1f;
        }
        writeCenteredInRegion(content, font, fontSize, safeText, left, right, y);
    }

    private String safePdfText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        byte[] asciiBytes = text.replaceAll("[\\r\\n\\t]+", " ").getBytes(StandardCharsets.US_ASCII);
        return new String(asciiBytes, StandardCharsets.US_ASCII).replace('?', ' ');
    }

    private byte[] readResource(String resourcePath) throws IOException {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            return inputStream == null ? null : inputStream.readAllBytes();
        }
    }

    private void drawImageIfPresent(PDDocument document, PDPageContentStream content, String resourcePath, float x, float y, float width, float height) throws IOException {
        byte[] bytes = readResource(resourcePath);
        if (bytes == null || bytes.length == 0) {
            return;
        }
        PDImageXObject image = PDImageXObject.createFromByteArray(document, bytes, resourcePath);
        content.drawImage(image, x, y, width, height);
    }

    private void setFillColor(PDPageContentStream content, int red, int green, int blue) throws IOException {
        content.setNonStrokingColor(red / 255f, green / 255f, blue / 255f);
    }

    private void setStrokeColor(PDPageContentStream content, int red, int green, int blue) throws IOException {
        content.setStrokingColor(red / 255f, green / 255f, blue / 255f);
    }

    private void drawBorder(PDPageContentStream content, float x, float y, float width, float height, int red, int green, int blue, float lineWidth) throws IOException {
        setStrokeColor(content, red, green, blue);
        content.setLineWidth(lineWidth);
        content.addRect(x, y, width, height);
        content.stroke();
    }

    private String buildCertificateCode(Enrollment enrollment) {
        String issued = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.systemDefault()).format(Instant.now());
        String studentPart = String.format("%04d", enrollment.getStudent().getId());
        String coursePart = String.format("%03d", enrollment.getCourse().getId());
        String randomPart = UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ENGLISH);
        return "EDS-" + issued + "-" + studentPart + "-" + coursePart + "-" + randomPart;
    }

    private String formatDate(Instant instant, String pattern) {
        return DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault()).format(instant);
    }
}
