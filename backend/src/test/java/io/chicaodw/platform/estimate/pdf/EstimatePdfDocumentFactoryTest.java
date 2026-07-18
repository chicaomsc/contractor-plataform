package io.chicaodw.platform.estimate.pdf;

import io.chicaodw.platform.common.entity.Address;
import io.chicaodw.platform.company.domain.Branding;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.domain.Settings;
import io.chicaodw.platform.estimate.domain.Estimate;
import io.chicaodw.platform.estimate.domain.EstimateItem;
import io.chicaodw.platform.estimate.domain.EstimateStatus;
import io.chicaodw.platform.estimate.domain.EstimateUnit;
import io.chicaodw.platform.estimate.domain.Material;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class EstimatePdfDocumentFactoryTest {

    private final EstimatePdfDocumentFactory factory = new EstimatePdfDocumentFactory();

    @Test
    void assemble_copiesCompanyAndBrandingData_withoutComputingAnything() {
        var estimate = draftlessEstimate();
        var company = company();
        var branding = branding("#1E40AF");
        var settings = settings();

        var doc = factory.assemble(estimate, company, branding, settings, null);

        assertThat(doc.seller().displayName()).isEqualTo("Acme Pinturas");
        assertThat(doc.seller().legalName()).isEqualTo("Acme Unipessoal Lda");
        assertThat(doc.seller().taxNumber()).isEqualTo("PT123456789");
        assertThat(doc.seller().phone()).isEqualTo("912345678");
        assertThat(doc.seller().email()).isEqualTo("hello@acme.example");
        assertThat(doc.seller().website()).isEqualTo("https://acme.example");
        assertThat(doc.seller().addressLine()).contains("Rua A", "Lisboa");
        assertThat(doc.seller().primaryColorHex()).isEqualTo("#1E40AF");
    }

    @Test
    void assemble_usesCompanyNameAsDisplayName_whenNoTradeName() {
        var company = company();
        company.setTradeName(null);
        var doc = factory.assemble(draftlessEstimate(), company, branding("#1E40AF"), settings(), null);

        assertThat(doc.seller().displayName()).isEqualTo("Acme Unipessoal Lda");
        assertThat(doc.seller().legalName()).isNull();
    }

    @Test
    void assemble_usesCustomerSnapshot_notLiveCustomerData() {
        var estimate = draftlessEstimate();
        estimate.setCustomerNameSnapshot("Jane Doe (at creation time)");
        estimate.setCustomerEmailSnapshot("jane-old@example.com");

        var doc = factory.assemble(estimate, company(), branding("#1E40AF"), settings(), null);

        assertThat(doc.customer().name()).isEqualTo("Jane Doe (at creation time)");
        assertThat(doc.customer().email()).isEqualTo("jane-old@example.com");
    }

    @Test
    void assemble_mapsItemsAndMaterials_withPersistedTotalsOnly() {
        var estimate = draftlessEstimate();
        estimate.addItem(item("Pintura de paredes", "45", EstimateUnit.M2, "12.50", "562.50"));
        estimate.addMaterial(material("Tinta branca", "8", EstimateUnit.UNIT, "18.00", "144.00"));

        var doc = factory.assemble(estimate, company(), branding("#1E40AF"), settings(), null);

        assertThat(doc.items()).hasSize(1);
        assertThat(doc.items().get(0).description()).isEqualTo("Pintura de paredes");
        assertThat(doc.items().get(0).quantityLabel()).isEqualTo("45");
        assertThat(doc.items().get(0).unitLabel()).isEqualTo("m²");
        assertThat(doc.items().get(0).totalLabel()).contains("562,50");

        assertThat(doc.materials()).hasSize(1);
        assertThat(doc.materials().get(0).description()).isEqualTo("Tinta branca");
        assertThat(doc.materials().get(0).totalLabel()).contains("144,00");
    }

    @Test
    void assemble_formatsPersistedTotals_usingEstimateCurrency_notAssumingEur() {
        var estimate = draftlessEstimate();
        estimate.setCurrency("USD");
        estimate.setTotal(new BigDecimal("184.50"));

        var doc = factory.assemble(estimate, company(), branding("#1E40AF"), settings(), null);

        assertThat(doc.summary().currency()).isEqualTo("USD");
        // Formatted with the tenant's own locale (pt-PT: comma decimal separator) but USD's own
        // symbol — never assumes EUR just because most of the fixtures in this suite use it.
        assertThat(doc.summary().totalLabel()).contains("184,50").doesNotContain("€");
    }

    @Test
    void assemble_marksDraftAndCancelled_correctly() {
        var draft = draftlessEstimate();
        draft.setStatus(EstimateStatus.DRAFT);
        var draftDoc = factory.assemble(draft, company(), branding("#1E40AF"), settings(), null);
        assertThat(draftDoc.draft()).isTrue();
        assertThat(draftDoc.cancelled()).isFalse();

        var cancelled = draftlessEstimate();
        cancelled.setStatus(EstimateStatus.CANCELLED);
        var cancelledDoc = factory.assemble(cancelled, company(), branding("#1E40AF"), settings(), null);
        assertThat(cancelledDoc.draft()).isFalse();
        assertThat(cancelledDoc.cancelled()).isTrue();

        var sent = draftlessEstimate();
        sent.setStatus(EstimateStatus.SENT);
        var sentDoc = factory.assemble(sent, company(), branding("#1E40AF"), settings(), null);
        assertThat(sentDoc.draft()).isFalse();
        assertThat(sentDoc.cancelled()).isFalse();
    }

    @Test
    void assemble_omitsOptionalFields_whenAbsent() {
        var estimate = draftlessEstimate();
        estimate.setDescription(null);
        estimate.setNotes(null);
        estimate.setTerms(null);
        estimate.setExpectedStartDate(null);
        estimate.setEstimatedDurationDays(null);
        var company = company();
        company.setWebsite(null);
        company.setPhone(null);

        var doc = factory.assemble(estimate, company, branding("#1E40AF"), settings(), null);

        assertThat(doc.metadata().description()).isNull();
        assertThat(doc.notes()).isNull();
        assertThat(doc.terms()).isNull();
        assertThat(doc.metadata().expectedStartDateLabel()).isNull();
        assertThat(doc.metadata().estimatedDurationLabel()).isNull();
        assertThat(doc.seller().website()).isNull();
        assertThat(doc.seller().phone()).isNull();
    }

    @Test
    void assemble_fallsBackToDefaultAccentColor_whenBrandingMissingOrInvalid() {
        var docNoBranding = factory.assemble(draftlessEstimate(), company(), null, settings(), null);
        assertThat(docNoBranding.seller().primaryColorHex()).isEqualTo("#2E2E2E");

        var docInvalidColor = factory.assemble(draftlessEstimate(), company(), branding("not-a-color"), settings(), null);
        assertThat(docInvalidColor.seller().primaryColorHex()).isEqualTo("#2E2E2E");
    }

    @Test
    void assemble_fallsBackToDefaultAccentColor_whenBrandingColorTooLight() {
        var doc = factory.assemble(draftlessEstimate(), company(), branding("#FEFEFE"), settings(), null);
        assertThat(doc.seller().primaryColorHex()).isEqualTo("#2E2E2E");
    }

    @Test
    void assemble_labelsUnits_forAllEnumValues() {
        var estimate = draftlessEstimate();
        for (EstimateUnit unit : EstimateUnit.values()) {
            estimate.addItem(item("Item " + unit, "1", unit, "10.00", "10.00"));
        }

        var doc = factory.assemble(estimate, company(), branding("#1E40AF"), settings(), null);

        assertThat(doc.items()).extracting("unitLabel")
                .containsExactlyInAnyOrder("Unidade", "Hora", "Dia", "m²", "m³", "Metro linear", "Fixo");
    }

    @Test
    void assemble_formatsDates_usingSettingsPatternAndLocale() {
        var estimate = draftlessEstimate();
        estimate.setIssueDate(LocalDate.of(2026, 7, 16));
        var settings = settings();
        settings.setDateFormat("dd/MM/yyyy");

        var doc = factory.assemble(estimate, company(), branding("#1E40AF"), settings, null);

        assertThat(doc.metadata().issueDateLabel()).isEqualTo("16/07/2026");
    }

    @Test
    void assemble_passesLogoBytesThrough_withoutModification() {
        byte[] logo = {1, 2, 3, 4};
        var doc = factory.assemble(draftlessEstimate(), company(), branding("#1E40AF"), settings(), logo);

        assertThat(doc.seller().logo()).isEqualTo(logo);
    }

    // ── fixtures ──────────────────────────────────────────────────────────────

    private static Estimate draftlessEstimate() {
        var estimate = new Estimate();
        estimate.setCompanyId(java.util.UUID.randomUUID());
        estimate.setCustomerId(java.util.UUID.randomUUID());
        estimate.setNumber("ORC-2026-0001");
        estimate.setTitle("Pintura interior");
        estimate.setDescription("Sala e quartos");
        estimate.setStatus(EstimateStatus.SENT);
        estimate.setIssueDate(LocalDate.of(2026, 7, 16));
        estimate.setValidUntil(LocalDate.of(2026, 8, 15));
        estimate.setNotes("Trabalho inclui preparação de superfície.");
        estimate.setTerms("Pagamento em 2 tranches.");
        estimate.setCurrency("EUR");
        estimate.setVatRate(new BigDecimal("23.00"));
        estimate.setUpfrontPercentage(new BigDecimal("50.00"));
        estimate.setLaborSubtotal(new BigDecimal("562.50"));
        estimate.setMaterialSubtotal(new BigDecimal("144.00"));
        estimate.setSubtotal(new BigDecimal("706.50"));
        estimate.setVatAmount(new BigDecimal("162.50"));
        estimate.setTotal(new BigDecimal("869.00"));
        estimate.setUpfrontAmount(new BigDecimal("434.50"));
        estimate.setRemainingAmount(new BigDecimal("434.50"));
        estimate.setCustomerNameSnapshot("Jane Doe");
        estimate.setCustomerEmailSnapshot("jane@example.com");
        estimate.setCustomerPhoneSnapshot("912345678");
        estimate.setCustomerTaxNumberSnapshot("PT987654321");
        estimate.setCustomerAddressSnapshot(Address.builder()
                .street("Rua do Cliente 10").city("Porto").postalCode("4000-001").country("PT").build());
        return estimate;
    }

    private static Company company() {
        var company = new Company();
        company.setName("Acme Unipessoal Lda");
        company.setTradeName("Acme Pinturas");
        company.setEmail("hello@acme.example");
        company.setPhone("912345678");
        company.setWebsite("https://acme.example");
        company.setTaxNumber("PT123456789");
        company.setCountry("PT");
        company.setAddress(Address.builder().street("Rua A").city("Lisboa").postalCode("1000-001").country("PT").build());
        return company;
    }

    private static Branding branding(String primaryColor) {
        var branding = new Branding();
        branding.setPrimaryColor(primaryColor);
        return branding;
    }

    private static Settings settings() {
        var settings = new Settings();
        settings.setLocale("pt-PT");
        settings.setDateFormat("dd/MM/yyyy");
        return settings;
    }

    private static EstimateItem item(String description, String quantity, EstimateUnit unit, String unitPrice, String total) {
        var item = new EstimateItem();
        item.setDescription(description);
        item.setQuantity(new BigDecimal(quantity));
        item.setUnit(unit);
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setTotal(new BigDecimal(total));
        return item;
    }

    private static Material material(String name, String quantity, EstimateUnit unit, String unitPrice, String total) {
        var material = new Material();
        material.setName(name);
        material.setQuantity(new BigDecimal(quantity));
        material.setUnit(unit);
        material.setUnitPrice(new BigDecimal(unitPrice));
        material.setTotal(new BigDecimal(total));
        return material;
    }
}
