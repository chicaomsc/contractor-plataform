package io.chicaodw.platform.estimate.pdf;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Draws an {@link EstimatePdfDocument} to PDF bytes using OpenPDF. Pure rendering: no
 * database access, no authentication awareness, no financial computation — every string
 * placed on the page was already resolved by {@link EstimatePdfDocumentFactory}.
 *
 * Uses {@link PdfPTable} for the item/material tables, which OpenPDF paginates
 * automatically without splitting a row across pages, repeating the header row (via
 * {@code setHeaderRows}) on every new page — this is the main reason OpenPDF was chosen
 * over a lower-level library for this MVP (see ADR-008).
 */
public class EstimatePdfRenderer {

    private static final float MARGIN = 36f;
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.BLACK);
    private static final Font SELLER_NAME_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
    private static final Font HEADING_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
    private static final Font BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, Color.BLACK);
    private static final Font MUTED_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, new Color(0x6B, 0x68, 0x60));
    private static final Font TABLE_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE);
    private static final Font TOTAL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.BLACK);
    private static final Font BADGE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

    public byte[] render(EstimatePdfDocument doc) {
        Document document = new Document(PageSize.A4, MARGIN, MARGIN, MARGIN + 6f, MARGIN + 14f);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, buffer);
            Color accent = parseColor(doc.seller().primaryColorHex());
            writer.setPageEvent(new FooterEvent(doc, accent));
            document.open();

            renderHeader(document, doc, accent);
            renderStatusBadge(document, doc, accent);
            renderEstimateMeta(document, doc);
            renderCustomer(document, doc);
            renderDescription(document, doc);
            renderLineTable(document, "Serviços / Mão de obra", doc.items(), accent);
            renderLineTable(document, "Materiais", doc.materials(), accent);
            renderSummary(document, doc, accent);
            renderNotesAndTerms(document, doc);
            renderSignatureArea(document);

            document.close();
            return buffer.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to render estimate PDF", e);
        }
    }

    // ── Sections ─────────────────────────────────────────────────────────────

    private void renderHeader(Document document, EstimatePdfDocument doc, Color accent) throws DocumentException {
        var seller = doc.seller();
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{1.2f, 3f});
        header.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (seller.logo() != null) {
            try {
                Image logo = Image.getInstance(seller.logo());
                logo.scaleToFit(90, 60);
                logoCell.addElement(logo);
            } catch (Exception e) {
                // Corrupt/unsupported image bytes — degrade to no logo rather than fail the PDF.
                logoCell.addElement(new Phrase(""));
            }
        }
        header.addCell(logoCell);

        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(Rectangle.NO_BORDER);
        infoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph name = new Paragraph(seller.displayName() != null ? seller.displayName() : "", SELLER_NAME_FONT);
        name.setAlignment(Element.ALIGN_RIGHT);
        infoCell.addElement(name);
        if (seller.legalName() != null) {
            infoCell.addElement(mutedRight(seller.legalName()));
        }
        if (seller.taxNumber() != null) {
            infoCell.addElement(mutedRight("NIF: " + seller.taxNumber()));
        }
        if (seller.addressLine() != null) {
            infoCell.addElement(mutedRight(seller.addressLine()));
        }
        if (seller.phone() != null) {
            infoCell.addElement(mutedRight(seller.phone()));
        }
        if (seller.email() != null) {
            infoCell.addElement(mutedRight(seller.email()));
        }
        if (seller.website() != null) {
            infoCell.addElement(mutedRight(seller.website()));
        }
        header.addCell(infoCell);

        document.add(header);
        document.add(rule(accent, 12f));

        Paragraph title = new Paragraph("Orçamento", TITLE_FONT);
        title.setSpacingBefore(8f);
        document.add(title);
    }

    private void renderStatusBadge(Document document, EstimatePdfDocument doc, Color accent) throws DocumentException {
        if (!doc.draft() && !doc.cancelled()) {
            return;
        }
        Color badgeColor = doc.cancelled() ? new Color(0xC0, 0x39, 0x2B) : accent;
        String label = doc.cancelled() ? "CANCELADO" : "RASCUNHO — DOCUMENTO NÃO ENVIADO";

        PdfPTable badge = new PdfPTable(1);
        badge.setWidthPercentage(100);
        badge.setSpacingBefore(6f);
        PdfPCell cell = new PdfPCell(new Phrase(label, BADGE_FONT));
        cell.setBackgroundColor(badgeColor);
        cell.setPadding(6f);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        badge.addCell(cell);
        document.add(badge);
    }

    private void renderEstimateMeta(Document document, EstimatePdfDocument doc) throws DocumentException {
        var meta = doc.metadata();
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8f);
        table.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        addMetaCell(table, "Número", meta.number());
        addMetaCell(table, "Status", meta.statusLabel());
        addMetaCell(table, "Data de emissão", meta.issueDateLabel());
        addMetaCell(table, "Válido até", meta.validUntilLabel());

        if (meta.expectedStartDateLabel() != null || meta.estimatedDurationLabel() != null) {
            if (meta.expectedStartDateLabel() != null) {
                addMetaCell(table, "Início previsto", meta.expectedStartDateLabel());
            } else {
                table.addCell(emptyCell());
            }
            if (meta.estimatedDurationLabel() != null) {
                addMetaCell(table, "Duração estimada", meta.estimatedDurationLabel());
            } else {
                table.addCell(emptyCell());
            }
            table.addCell(emptyCell());
            table.addCell(emptyCell());
        }

        document.add(table);

        Paragraph jobTitle = new Paragraph(meta.title(), HEADING_FONT);
        jobTitle.setSpacingBefore(6f);
        document.add(jobTitle);
    }

    private void renderCustomer(Document document, EstimatePdfDocument doc) throws DocumentException {
        var customer = doc.customer();
        Paragraph heading = new Paragraph("Cliente", HEADING_FONT);
        heading.setSpacingBefore(8f);
        document.add(heading);

        Paragraph name = new Paragraph(customer.name(), BODY_FONT);
        name.setSpacingBefore(4f);
        document.add(name);

        StringBuilder contact = new StringBuilder();
        appendJoined(contact, customer.taxNumber() != null ? "NIF: " + customer.taxNumber() : null);
        appendJoined(contact, customer.phone());
        appendJoined(contact, customer.email());
        if (!contact.isEmpty()) {
            document.add(new Paragraph(contact.toString(), MUTED_FONT));
        }
        if (customer.addressLine() != null) {
            document.add(new Paragraph(customer.addressLine(), MUTED_FONT));
        }
    }

    private void renderDescription(Document document, EstimatePdfDocument doc) throws DocumentException {
        if (doc.metadata().description() == null) {
            return;
        }
        Paragraph description = new Paragraph(doc.metadata().description(), BODY_FONT);
        description.setSpacingBefore(4f);
        document.add(description);
    }

    private void renderLineTable(Document document, String heading, List<EstimatePdfDocument.LineItem> lines, Color accent) throws DocumentException {
        if (lines.isEmpty()) {
            return;
        }

        Paragraph title = new Paragraph(heading, HEADING_FONT);
        title.setSpacingBefore(10f);
        document.add(title);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6f);
        table.setWidths(new float[]{3.4f, 1f, 1.2f, 1.2f, 1.2f});
        table.setHeaderRows(1);
        // A row must never be cut mid-text across a page break — the whole row moves to
        // the next page instead if it doesn't fit (setSplitRows(false) is what enforces
        // this in OpenPDF; the name is counter-intuitive — "true" would allow splitting).
        table.setSplitLate(true);
        table.setSplitRows(false);

        addHeaderCell(table, "Descrição", accent, Element.ALIGN_LEFT);
        addHeaderCell(table, "Qtd.", accent, Element.ALIGN_RIGHT);
        addHeaderCell(table, "Unidade", accent, Element.ALIGN_LEFT);
        addHeaderCell(table, "Preço unit.", accent, Element.ALIGN_RIGHT);
        addHeaderCell(table, "Total", accent, Element.ALIGN_RIGHT);

        boolean shaded = false;
        for (var line : lines) {
            Color background = shaded ? new Color(0xF3, 0xF1, 0xEC) : Color.WHITE;
            addBodyCell(table, line.description(), Element.ALIGN_LEFT, background);
            addBodyCell(table, line.quantityLabel(), Element.ALIGN_RIGHT, background);
            addBodyCell(table, line.unitLabel(), Element.ALIGN_LEFT, background);
            addBodyCell(table, line.unitPriceLabel(), Element.ALIGN_RIGHT, background);
            addBodyCell(table, line.totalLabel(), Element.ALIGN_RIGHT, background);
            shaded = !shaded;
        }

        document.add(table);
    }

    private void renderSummary(Document document, EstimatePdfDocument doc, Color accent) throws DocumentException {
        var summary = doc.summary();
        Paragraph heading = new Paragraph("Resumo financeiro", HEADING_FONT);
        heading.setSpacingBefore(10f);
        document.add(heading);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(60);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setSpacingBefore(6f);
        table.setKeepTogether(true);

        addSummaryRow(table, "Mão de obra", summary.laborSubtotalLabel(), false);
        addSummaryRow(table, "Materiais", summary.materialSubtotalLabel(), false);
        addSummaryRow(table, "Subtotal", summary.subtotalLabel(), false);
        addSummaryRow(table, summary.vatLabel(), summary.vatAmountLabel(), false);
        addSummaryRow(table, "Total", summary.totalLabel(), true);
        addSummaryRow(table, summary.upfrontLabel(), summary.upfrontAmountLabel(), false);
        addSummaryRow(table, "Saldo restante", summary.remainingLabel(), false);

        document.add(table);
    }

    private void renderNotesAndTerms(Document document, EstimatePdfDocument doc) throws DocumentException {
        if (doc.notes() != null) {
            Paragraph heading = new Paragraph("Observações", HEADING_FONT);
            heading.setSpacingBefore(12f);
            document.add(heading);
            document.add(new Paragraph(doc.notes(), BODY_FONT));
        }
        if (doc.terms() != null) {
            Paragraph heading = new Paragraph("Condições", HEADING_FONT);
            heading.setSpacingBefore(10f);
            document.add(heading);
            document.add(new Paragraph(doc.terms(), BODY_FONT));
        }
    }

    private void renderSignatureArea(Document document) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setKeepTogether(true);
        table.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        table.addCell(signatureCell("Nome do cliente"));
        table.addCell(signatureCell("Data"));
        table.addCell(signatureCell("Assinatura"));
        table.addCell(emptyCell());

        document.add(table);
    }

    // ── Cell/paragraph builders ──────────────────────────────────────────────

    private static Paragraph mutedRight(String text) {
        Paragraph paragraph = new Paragraph(text, MUTED_FONT);
        paragraph.setAlignment(Element.ALIGN_RIGHT);
        return paragraph;
    }

    private static void appendJoined(StringBuilder builder, String value) {
        if (value == null) return;
        if (!builder.isEmpty()) builder.append("   ·   ");
        builder.append(value);
    }

    private static void addMetaCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(4f);
        Paragraph p = new Paragraph();
        p.add(new Chunk(label.toUpperCase() + "\n", MUTED_FONT));
        p.add(new Chunk(value != null ? value : "—", BODY_FONT));
        cell.addElement(p);
        table.addCell(cell);
    }

    private static PdfPCell emptyCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private static void addHeaderCell(PdfPTable table, String text, Color accent, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_HEADER_FONT));
        cell.setBackgroundColor(accent);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(alignment);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    private static void addBodyCell(PdfPTable table, String text, int alignment, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", BODY_FONT));
        cell.setPadding(4f);
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(background);
        cell.setBorderColor(new Color(0xE8, 0xE5, 0xDF));
        cell.setBorderWidthLeft(0);
        cell.setBorderWidthRight(0);
        table.addCell(cell);
    }

    private static void addSummaryRow(PdfPTable table, String label, String value, boolean emphasize) {
        Font labelFont = emphasize ? TOTAL_FONT : BODY_FONT;
        Font valueFont = emphasize ? TOTAL_FONT : BODY_FONT;

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(emphasize ? Rectangle.TOP : Rectangle.NO_BORDER);
        labelCell.setPadding(4f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(emphasize ? Rectangle.TOP : Rectangle.NO_BORDER);
        valueCell.setPadding(4f);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private static PdfPCell signatureCell(String label) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(10f);
        cell.setPaddingRight(16f);
        Paragraph line = new Paragraph("_______________________________", BODY_FONT);
        cell.addElement(line);
        cell.addElement(new Paragraph(label, MUTED_FONT));
        return cell;
    }

    private static Paragraph rule(Color color, float spacingAfter) {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(2f);
        cell.setBackgroundColor(color);
        cell.setBorder(Rectangle.NO_BORDER);
        line.addCell(cell);
        Paragraph wrapper = new Paragraph();
        wrapper.add(line);
        wrapper.setSpacingAfter(spacingAfter);
        return wrapper;
    }

    private static Color parseColor(String hex) {
        try {
            return Color.decode(hex);
        } catch (Exception e) {
            return new Color(0x2E, 0x2E, 0x2E);
        }
    }

    /** Footer: estimate number, seller name, page number — repeated on every page. */
    private static class FooterEvent extends PdfPageEventHelper {

        private final EstimatePdfDocument doc;
        private final Color accent;

        FooterEvent(EstimatePdfDocument doc, Color accent) {
            this.doc = doc;
            this.accent = accent;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            String seller = doc.seller().displayName() != null ? doc.seller().displayName() : "";
            String text = "Orçamento %s   ·   %s   ·   Página %d"
                    .formatted(doc.metadata().number(), seller, writer.getPageNumber());
            PdfPTable footer = new PdfPTable(1);
            footer.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
            PdfPCell cell = new PdfPCell(new Phrase(text, MUTED_FONT));
            cell.setBorder(Rectangle.TOP);
            cell.setBorderColor(accent);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPaddingTop(6f);
            footer.addCell(cell);
            footer.writeSelectedRows(0, -1,
                    document.leftMargin(),
                    document.bottomMargin() - 6f,
                    writer.getDirectContent());
        }
    }
}
