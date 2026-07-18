package io.chicaodw.platform.estimate.pdf;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EstimatePdfRendererTest {

    private final EstimatePdfRenderer renderer = new EstimatePdfRenderer();

    @Test
    void render_producesNonEmptyBytes_startingWithPdfSignature() {
        byte[] bytes = renderer.render(document(1, 1, false, false));

        assertThat(bytes).isNotEmpty();
        assertThat(new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void render_producesADocumentTheLibraryCanReopen() throws Exception {
        byte[] bytes = renderer.render(document(1, 1, false, false));

        try (PdfReader reader = new PdfReader(bytes)) {
            assertThat(reader.getNumberOfPages()).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    void render_includesEstimateNumberAndCustomerName_inExtractedText() throws Exception {
        byte[] bytes = renderer.render(document(1, 1, false, false));
        String text = extractAllText(bytes);

        assertThat(text).contains("ORC-2026-0001");
        assertThat(text).contains("Jane Doe");
    }

    @Test
    void render_includesPersistedTotals_inExtractedText() throws Exception {
        byte[] bytes = renderer.render(document(1, 1, false, false));
        String text = extractAllText(bytes);

        assertThat(text).contains("184,50");
    }

    @Test
    void render_handlesMultipleItemsAndMaterials() throws Exception {
        byte[] bytes = renderer.render(document(6, 6, false, false));
        String text = extractAllText(bytes);

        for (int i = 0; i < 6; i++) {
            assertThat(text).contains("Item " + i);
            assertThat(text).contains("Material " + i);
        }
    }

    @Test
    void render_spansMultiplePages_whenManyItems() throws Exception {
        byte[] bytes = renderer.render(document(80, 80, false, false));

        try (PdfReader reader = new PdfReader(bytes)) {
            assertThat(reader.getNumberOfPages()).isGreaterThan(1);
        }
    }

    @Test
    void render_neverSplitsARowAcrossPages() throws Exception {
        // Regression test: PdfPTable#setSplitRows(true) would let OpenPDF cut a row's text
        // mid-line at the page boundary. Each row here carries a START/END marker; if a row
        // is ever split, the two markers end up on different pages.
        List<EstimatePdfDocument.LineItem> items = java.util.stream.IntStream.range(0, 30)
                .mapToObj(i -> new EstimatePdfDocument.LineItem(
                        "START" + i + " " + "texto de preenchimento ".repeat(6) + "END" + i,
                        "1", "Unidade", "10,00 €", "10,00 €"))
                .toList();
        var base = document(0, 0, false, false);
        var doc = new EstimatePdfDocument(
                base.seller(), base.metadata(), base.customer(), items, List.of(),
                base.summary(), base.notes(), base.terms(), base.draft(), base.cancelled());

        byte[] bytes = renderer.render(doc);
        try (PdfReader reader = new PdfReader(bytes)) {
            assertThat(reader.getNumberOfPages()).isGreaterThan(1);
            for (int i = 0; i < 30; i++) {
                int startPage = findPageContaining(reader, "START" + i);
                int endPage = findPageContaining(reader, "END" + i);
                assertThat(startPage)
                        .as("row %d's START/END markers must land on the same page — a row must never be split", i)
                        .isEqualTo(endPage);
            }
        }
    }

    private static int findPageContaining(PdfReader reader, String marker) throws Exception {
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            if (extractor.getTextFromPage(page).contains(marker)) {
                return page;
            }
        }
        throw new AssertionError("marker not found on any page: " + marker);
    }

    @Test
    void render_repeatsTableHeader_onEveryPageTheTableSpans() throws Exception {
        byte[] bytes = renderer.render(document(80, 0, false, false));

        try (PdfReader reader = new PdfReader(bytes)) {
            assertThat(reader.getNumberOfPages()).isGreaterThan(1);
            // The item table spans at least the first two pages — PdfPTable#setHeaderRows(1)
            // must repeat the "Descrição" column header on both, not just the first.
            String page1 = new PdfTextExtractor(reader).getTextFromPage(1);
            String page2 = new PdfTextExtractor(reader).getTextFromPage(2);
            assertThat(page1).contains("Descrição");
            assertThat(page2).contains("Descrição");
        }
    }

    @Test
    void render_worksWithoutLogo() {
        var doc = document(1, 1, false, false);
        byte[] bytes = renderer.render(doc);

        assertThat(bytes).isNotEmpty();
    }

    @Test
    void render_worksWithLogo() throws Exception {
        byte[] pngLogo = tinyPng();
        var base = document(1, 1, false, false);
        var withLogo = new EstimatePdfDocument(
                new EstimatePdfDocument.SellerInfo(
                        base.seller().displayName(), base.seller().legalName(), base.seller().taxNumber(),
                        base.seller().phone(), base.seller().email(), base.seller().website(),
                        base.seller().addressLine(), pngLogo, base.seller().primaryColorHex()
                ),
                base.metadata(), base.customer(), base.items(), base.materials(), base.summary(),
                base.notes(), base.terms(), base.draft(), base.cancelled()
        );

        byte[] bytes = renderer.render(withLogo);

        assertThat(bytes).isNotEmpty();
        try (PdfReader reader = new PdfReader(bytes)) {
            assertThat(reader.getNumberOfPages()).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    void render_handlesLongDescriptions_withoutError() throws Exception {
        var longText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ".repeat(40);
        var base = document(1, 1, false, false);
        var withLongNotes = new EstimatePdfDocument(
                base.seller(), base.metadata(), base.customer(), base.items(), base.materials(),
                base.summary(), longText, longText, base.draft(), base.cancelled()
        );

        byte[] bytes = renderer.render(withLongNotes);
        String text = extractAllText(bytes);

        assertThat(bytes).isNotEmpty();
        assertThat(text).contains("Lorem ipsum");
    }

    @Test
    void render_marksDraft_withVisibleBadge() throws Exception {
        byte[] bytes = renderer.render(document(1, 1, true, false));
        String text = extractAllText(bytes);

        assertThat(text).contains("RASCUNHO");
    }

    @Test
    void render_marksCancelled_withVisibleBadge() throws Exception {
        byte[] bytes = renderer.render(document(1, 1, false, true));
        String text = extractAllText(bytes);

        assertThat(text).contains("CANCELADO");
    }

    @Test
    void render_omitsBadge_forNonDraftNonCancelled() throws Exception {
        byte[] bytes = renderer.render(document(1, 1, false, false));
        String text = extractAllText(bytes);

        assertThat(text).doesNotContain("RASCUNHO").doesNotContain("CANCELADO");
    }

    @Test
    void render_handlesNoItemsAndNoMaterials() {
        byte[] bytes = renderer.render(document(0, 0, false, false));

        assertThat(bytes).isNotEmpty();
    }

    // ── fixtures ──────────────────────────────────────────────────────────────

    private static String extractAllText(byte[] pdfBytes) throws Exception {
        try (PdfReader reader = new PdfReader(pdfBytes)) {
            StringBuilder text = new StringBuilder();
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page)).append('\n');
            }
            return text.toString();
        }
    }

    private static EstimatePdfDocument document(int itemCount, int materialCount, boolean draft, boolean cancelled) {
        return new EstimatePdfDocument(
                new EstimatePdfDocument.SellerInfo(
                        "Acme Pinturas", "Acme Unipessoal Lda", "PT123456789",
                        "912345678", "hello@acme.example", "https://acme.example",
                        "Rua A, 1000-001, Lisboa, PT", null, "#1E40AF"
                ),
                new EstimatePdfDocument.EstimateMetadata(
                        "ORC-2026-0001", "Enviado", "16/07/2026", "15/08/2026",
                        null, null, "Pintura interior", "Sala e quartos"
                ),
                new EstimatePdfDocument.CustomerSnapshot(
                        "Jane Doe", "PT987654321", "912345678", "jane@example.com",
                        "Rua do Cliente 10, 4000-001, Porto, PT"
                ),
                items(itemCount),
                materials(materialCount),
                new EstimatePdfDocument.FinancialSummary(
                        "EUR", "562,50 €", "144,00 €", "706,50 €",
                        "IVA (23%)", "162,50 €", "184,50 €",
                        "Entrada (50%)", "92,25 €", "92,25 €"
                ),
                "Trabalho inclui preparação de superfície.",
                "Pagamento em 2 tranches.",
                draft,
                cancelled
        );
    }

    private static List<EstimatePdfDocument.LineItem> items(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> new EstimatePdfDocument.LineItem("Item " + i, "1", "Unidade", "10,00 €", "10,00 €"))
                .toList();
    }

    private static List<EstimatePdfDocument.LineItem> materials(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> new EstimatePdfDocument.LineItem("Material " + i, "2", "Unidade", "5,00 €", "10,00 €"))
                .toList();
    }

    private static byte[] tinyPng() {
        // Minimal valid 1x1 transparent PNG.
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
                (byte) 0x89, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x44, 0x41,
                0x54, 0x78, (byte) 0x9C, 0x62, 0x00, 0x01, 0x00, 0x00,
                0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00,
                0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE,
                0x42, 0x60, (byte) 0x82
        };
    }
}
