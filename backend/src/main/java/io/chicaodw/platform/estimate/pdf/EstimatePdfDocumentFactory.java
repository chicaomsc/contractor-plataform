package io.chicaodw.platform.estimate.pdf;

import io.chicaodw.platform.common.entity.Address;
import io.chicaodw.platform.company.domain.Branding;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.domain.Settings;
import io.chicaodw.platform.estimate.domain.Estimate;
import io.chicaodw.platform.estimate.domain.EstimateItem;
import io.chicaodw.platform.estimate.domain.EstimateStatus;
import io.chicaodw.platform.estimate.domain.Material;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

/**
 * Transforms domain data (Estimate + its snapshot, Company, Branding, Settings) into an
 * {@link EstimatePdfDocument} — the immutable model the renderer draws. No Spring
 * dependency, no database access, easily unit testable in isolation.
 *
 * Every monetary/percentage/date value here is copied and formatted from data already
 * persisted by the domain layer (EstimateCalculationService, EstimateService) — this class
 * never multiplies, sums, or otherwise recomputes a financial figure. Formatting BigDecimal
 * values goes through {@link NumberFormat#format(Object)}, which has a dedicated BigDecimal
 * code path (no conversion to {@code double}).
 */
public class EstimatePdfDocumentFactory {

    private static final String FALLBACK_ACCENT_COLOR = "#2E2E2E";
    private static final java.util.regex.Pattern HEX_COLOR = java.util.regex.Pattern.compile("^#[0-9A-Fa-f]{6}$");

    public EstimatePdfDocument assemble(
            Estimate estimate,
            Company company,
            Branding branding,
            Settings settings,
            byte[] logoBytes
    ) {
        Locale locale = resolveLocale(settings);
        DateTimeFormatter dateFormatter = resolveDateFormatter(settings, locale);

        return new EstimatePdfDocument(
                buildSeller(company, branding, logoBytes),
                buildMetadata(estimate, dateFormatter),
                buildCustomer(estimate),
                buildItemLines(estimate.getItems(), locale, estimate.getCurrency()),
                buildMaterialLines(estimate.getMaterials(), locale, estimate.getCurrency()),
                buildSummary(estimate, locale),
                blankToNull(estimate.getNotes()),
                blankToNull(estimate.getTerms()),
                estimate.getStatus() == EstimateStatus.DRAFT,
                estimate.getStatus() == EstimateStatus.CANCELLED
        );
    }

    // ── Seller ───────────────────────────────────────────────────────────────

    private EstimatePdfDocument.SellerInfo buildSeller(Company company, Branding branding, byte[] logoBytes) {
        String displayName = firstNonBlank(company.getTradeName(), company.getName());
        boolean hasDistinctLegalName = company.getTradeName() != null
                && !company.getTradeName().isBlank()
                && !company.getTradeName().equals(company.getName());
        String legalName = hasDistinctLegalName ? company.getName() : null;

        return new EstimatePdfDocument.SellerInfo(
                displayName,
                legalName,
                blankToNull(company.getTaxNumber()),
                blankToNull(company.getPhone()),
                blankToNull(company.getEmail()),
                blankToNull(company.getWebsite()),
                formatAddressLine(company.getAddress()),
                logoBytes,
                resolveAccentColor(branding)
        );
    }

    private String resolveAccentColor(Branding branding) {
        String color = branding != null ? branding.getPrimaryColor() : null;
        if (color == null || !HEX_COLOR.matcher(color).matches()) {
            return FALLBACK_ACCENT_COLOR;
        }
        return isTooLight(color) ? FALLBACK_ACCENT_COLOR : color;
    }

    private boolean isTooLight(String hex) {
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        double perceivedBrightness = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
        return perceivedBrightness > 0.75;
    }

    // ── Metadata ─────────────────────────────────────────────────────────────

    private EstimatePdfDocument.EstimateMetadata buildMetadata(Estimate estimate, DateTimeFormatter dateFormatter) {
        return new EstimatePdfDocument.EstimateMetadata(
                estimate.getNumber(),
                EstimateStatusLabels.label(estimate.getStatus()),
                formatDate(estimate.getIssueDate(), dateFormatter),
                formatDate(estimate.getValidUntil(), dateFormatter),
                formatDate(estimate.getExpectedStartDate(), dateFormatter),
                estimate.getEstimatedDurationDays() != null
                        ? estimate.getEstimatedDurationDays() + " dias"
                        : null,
                estimate.getTitle(),
                blankToNull(estimate.getDescription())
        );
    }

    // ── Customer snapshot ────────────────────────────────────────────────────

    private EstimatePdfDocument.CustomerSnapshot buildCustomer(Estimate estimate) {
        return new EstimatePdfDocument.CustomerSnapshot(
                estimate.getCustomerNameSnapshot(),
                blankToNull(estimate.getCustomerTaxNumberSnapshot()),
                blankToNull(estimate.getCustomerPhoneSnapshot()),
                blankToNull(estimate.getCustomerEmailSnapshot()),
                formatAddressLine(estimate.getCustomerAddressSnapshot())
        );
    }

    // ── Line items / materials ──────────────────────────────────────────────

    private List<EstimatePdfDocument.LineItem> buildItemLines(List<EstimateItem> items, Locale locale, String currency) {
        return items.stream()
                .map(item -> new EstimatePdfDocument.LineItem(
                        item.getDescription(),
                        formatQuantity(item.getQuantity()),
                        EstimateUnitLabels.label(item.getUnit()),
                        formatMoney(item.getUnitPrice(), currency, locale),
                        formatMoney(item.getTotal(), currency, locale)
                ))
                .toList();
    }

    private List<EstimatePdfDocument.LineItem> buildMaterialLines(List<Material> materials, Locale locale, String currency) {
        return materials.stream()
                .map(material -> new EstimatePdfDocument.LineItem(
                        material.getName(),
                        formatQuantity(material.getQuantity()),
                        EstimateUnitLabels.label(material.getUnit()),
                        formatMoney(material.getUnitPrice(), currency, locale),
                        formatMoney(material.getTotal(), currency, locale)
                ))
                .toList();
    }

    // ── Financial summary ───────────────────────────────────────────────────

    private EstimatePdfDocument.FinancialSummary buildSummary(Estimate estimate, Locale locale) {
        String currency = estimate.getCurrency();
        return new EstimatePdfDocument.FinancialSummary(
                currency,
                formatMoney(estimate.getLaborSubtotal(), currency, locale),
                formatMoney(estimate.getMaterialSubtotal(), currency, locale),
                formatMoney(estimate.getSubtotal(), currency, locale),
                "IVA (" + formatPercentage(estimate.getVatRate()) + ")",
                formatMoney(estimate.getVatAmount(), currency, locale),
                formatMoney(estimate.getTotal(), currency, locale),
                "Entrada (" + formatPercentage(estimate.getUpfrontPercentage()) + ")",
                formatMoney(estimate.getUpfrontAmount(), currency, locale),
                formatMoney(estimate.getRemainingAmount(), currency, locale)
        );
    }

    // ── Formatting helpers (presentation only — no financial computation) ──────

    private static Locale resolveLocale(Settings settings) {
        String tag = settings != null ? settings.getLocale() : null;
        if (tag == null || tag.isBlank()) {
            return Locale.forLanguageTag("pt-PT");
        }
        return Locale.forLanguageTag(tag);
    }

    private static DateTimeFormatter resolveDateFormatter(Settings settings, Locale locale) {
        String pattern = settings != null ? settings.getDateFormat() : null;
        if (pattern == null || pattern.isBlank()) {
            pattern = "dd/MM/yyyy";
        }
        return DateTimeFormatter.ofPattern(pattern, locale);
    }

    private static String formatDate(LocalDate date, DateTimeFormatter formatter) {
        return date != null ? date.format(formatter) : null;
    }

    private static String formatMoney(BigDecimal value, String currencyCode, Locale locale) {
        NumberFormat format = NumberFormat.getCurrencyInstance(locale);
        try {
            format.setCurrency(Currency.getInstance(currencyCode));
        } catch (IllegalArgumentException ignored) {
            // Snapshot currency code isn't ISO-recognized in this JVM — keep the locale default
            // rather than failing PDF generation over a display-only formatting concern.
        }
        return format.format(value);
    }

    private static String formatPercentage(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString() + "%";
    }

    private static String formatQuantity(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String formatAddressLine(Address address) {
        if (address == null) return null;
        String line = java.util.stream.Stream.of(
                        address.getStreet(),
                        address.getPostalCode(),
                        address.getCity(),
                        address.getRegion(),
                        address.getCountry()
                )
                .filter(part -> part != null && !part.isBlank())
                .reduce((a, b) -> a + ", " + b)
                .orElse(null);
        return blankToNull(line);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
