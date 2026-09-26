package com.glbajaj.campuscare.service;

import com.glbajaj.campuscare.entity.Issue;
import com.glbajaj.campuscare.util.EntityMapper;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** CSV and PDF export of the (filtered) issues, generated from real database rows. */
@Service
public class ExportService {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final int PDF_ROW_LIMIT = 1000;
    private static final Color MAROON = new Color(0x5B, 0x1A, 0x22);

    private final IssueService issueService;
    private final SlaService slaService;

    public ExportService(IssueService issueService, SlaService slaService) {
        this.issueService = issueService;
        this.slaService = slaService;
    }

    // ------------------------------------------------------------------ CSV
    @Transactional(readOnly = true)
    public byte[] csv(IssueFilter filter) {
        StringBuilder sb = new StringBuilder("\uFEFF");   // BOM so Excel opens UTF-8 correctly
        sb.append("Issue No,Title,Category,Location,Priority,Status,Student,Student ID,Assigned Staff,Department,Created At,Resolved At,Overdue\r\n");
        for (Issue i : issueService.findAll(filter)) {
            sb.append(String.join(",",
                    cell(i.getIssueNumber()), cell(i.getTitle()), cell(EntityMapper.categoryPath(i.getCategory())),
                    cell(EntityMapper.issueLocation(i)), cell(i.getPriority().name()), cell(i.getStatus().name()),
                    cell(i.getStudent().getUser().getName()), cell(i.getStudent().getStudentId()),
                    cell(i.getAssignedStaff() == null ? "" : i.getAssignedStaff().getUser().getName()),
                    cell(i.getAssignedDepartment() == null ? "" : i.getAssignedDepartment().getName()),
                    cell(fmt(i.getCreatedAt())), cell(fmt(i.getResolvedAt())), cell(slaService.isOverdue(i) ? "Yes" : "No")))
              .append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Escapes a CSV value and neutralises spreadsheet formula injection (values starting with = + - @). */
    private static String cell(String v) {
        if (v == null) return "";
        String s = v;
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) s = "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }

    // ------------------------------------------------------------------ PDF
    @Transactional(readOnly = true)
    public byte[] pdf(IssueFilter filter, String rangeLabel) {
        List<Issue> issues = issueService.findAll(filter);
        Document doc = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            // Header: logo (proportions preserved by scaleToFit) + title
            PdfPTable header = new PdfPTable(new float[]{1.2f, 8.8f});
            header.setWidthPercentage(100);
            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);
            try (InputStream in = new ClassPathResource("branding/gl-bajaj-logo.png").getInputStream()) {
                Image logo = Image.getInstance(in.readAllBytes());
                logo.scaleToFit(72, 54);
                logoCell.addElement(logo);
            }
            header.addCell(logoCell);
            PdfPCell titleCell = new PdfPCell();
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.addElement(new Paragraph("GL Bajaj CampusCare", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, MAROON)));
            titleCell.addElement(new Paragraph("Campus Issue Management Report", FontFactory.getFont(FontFactory.HELVETICA, 12, Color.DARK_GRAY)));
            titleCell.addElement(new Paragraph("Period: " + rangeLabel + "   |   Issues: " + issues.size() + "   |   Generated: "
                    + FMT.format(LocalDateTime.now()), FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY)));
            header.addCell(titleCell);
            doc.add(header);
            doc.add(new Paragraph(" "));

            Font head = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
            PdfPTable table = new PdfPTable(new float[]{1.1f, 3.2f, 2.4f, 2.8f, 1.1f, 1.4f, 1.8f, 1.8f, 1.6f});
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            for (String h : new String[]{"Issue No", "Title", "Category", "Location", "Priority", "Status", "Student", "Assigned To", "Created"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, head));
                c.setBackgroundColor(MAROON);
                c.setPadding(4);
                table.addCell(c);
            }
            int shown = 0;
            for (Issue i : issues) {
                if (shown++ >= PDF_ROW_LIMIT) break;
                String[] row = {i.getIssueNumber(), i.getTitle(), EntityMapper.categoryPath(i.getCategory()),
                        EntityMapper.issueLocation(i), i.getPriority().name(), i.getStatus().name().replace('_', ' '),
                        i.getStudent().getUser().getName(), i.getAssignedStaff() == null ? "-" : i.getAssignedStaff().getUser().getName(),
                        fmt(i.getCreatedAt())};
                for (String v : row) {
                    PdfPCell c = new PdfPCell(new Phrase(v == null ? "" : v, body));
                    c.setPadding(3);
                    table.addCell(c);
                }
            }
            doc.add(table);
            if (issues.size() > PDF_ROW_LIMIT) {
                doc.add(new Paragraph("Only the first " + PDF_ROW_LIMIT + " issues are shown. Use the CSV export for the full list.", body));
            }
            doc.add(new Paragraph("Generated by GL Bajaj CampusCare - a college project. SLA targets are configurable demo values, not official GL Bajaj policy.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7, Color.GRAY)));
            doc.close();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("Could not generate the PDF report", e);
        }
        return out.toByteArray();
    }

    private static String fmt(LocalDateTime t) { return t == null ? "" : FMT.format(t); }
}
