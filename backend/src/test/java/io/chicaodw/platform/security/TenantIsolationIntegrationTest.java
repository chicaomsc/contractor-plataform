package io.chicaodw.platform.security;

import io.chicaodw.platform.AbstractIntegrationTest;
import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.common.storage.StorageService;
import io.chicaodw.platform.company.api.dto.UpdateBrandingRequest;
import io.chicaodw.platform.company.api.dto.UpdateCompanyRequest;
import io.chicaodw.platform.company.api.dto.UpdateSettingsRequest;
import io.chicaodw.platform.company.application.BrandingService;
import io.chicaodw.platform.company.application.CompanyService;
import io.chicaodw.platform.company.application.SettingsService;
import io.chicaodw.platform.company.domain.Branding;
import io.chicaodw.platform.company.domain.Company;
import io.chicaodw.platform.company.domain.Settings;
import io.chicaodw.platform.company.infrastructure.persistence.BrandingRepository;
import io.chicaodw.platform.company.infrastructure.persistence.CompanyRepository;
import io.chicaodw.platform.company.infrastructure.persistence.SettingsRepository;
import io.chicaodw.platform.customer.api.dto.CreateCustomerRequest;
import io.chicaodw.platform.customer.api.dto.UpdateCustomerRequest;
import io.chicaodw.platform.customer.application.CustomerService;
import io.chicaodw.platform.customer.infrastructure.persistence.CustomerRepository;
import io.chicaodw.platform.estimate.api.dto.ChangeEstimateStatusRequest;
import io.chicaodw.platform.estimate.api.dto.CreateEstimateRequest;
import io.chicaodw.platform.estimate.api.dto.EstimateItemRequest;
import io.chicaodw.platform.estimate.api.dto.UpdateEstimateRequest;
import io.chicaodw.platform.estimate.application.EstimateService;
import io.chicaodw.platform.estimate.domain.EstimateStatus;
import io.chicaodw.platform.estimate.domain.EstimateUnit;
import io.chicaodw.platform.estimate.infrastructure.persistence.EstimateRepository;
import io.chicaodw.platform.gallery.api.dto.CreateGalleryRequest;
import io.chicaodw.platform.gallery.api.dto.UpdateGalleryRequest;
import io.chicaodw.platform.gallery.application.GalleryService;
import io.chicaodw.platform.gallery.infrastructure.persistence.GalleryRepository;
import io.chicaodw.platform.servicecatalog.api.dto.CreateServiceRequest;
import io.chicaodw.platform.servicecatalog.api.dto.UpdateServiceRequest;
import io.chicaodw.platform.servicecatalog.application.ServiceCatalogService;
import io.chicaodw.platform.servicecatalog.infrastructure.persistence.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantIsolationIntegrationTest extends AbstractIntegrationTest {

    @Autowired CompanyRepository companyRepository;
    @Autowired BrandingRepository brandingRepository;
    @Autowired SettingsRepository settingsRepository;
    @Autowired ServiceRepository serviceRepository;
    @Autowired GalleryRepository galleryRepository;
    @Autowired CompanyService companyService;
    @Autowired BrandingService brandingService;
    @Autowired SettingsService settingsService;
    @Autowired ServiceCatalogService serviceCatalogService;
    @Autowired GalleryService galleryService;
    @Autowired CustomerService customerService;
    @Autowired CustomerRepository customerRepository;
    @Autowired EstimateService estimateService;
    @Autowired EstimateRepository estimateRepository;

    @MockitoBean StorageService storageService;

    private UUID companyA;
    private UUID companyB;

    @BeforeEach
    void setUp() {
        var suffix = UUID.randomUUID().toString();
        companyA = createCompany("Tenant A", "tenant-a-" + suffix).getId();
        companyB = createCompany("Tenant B", "tenant-b-" + suffix).getId();
        createBranding(companyA, "#111111");
        createBranding(companyB, "#222222");
        createSettings(companyA, "EUR");
        createSettings(companyB, "USD");
    }

    @Test
    void companyProfile_updatesOnlyRequestedCompany() {
        companyService.updateProfile(companyA, new UpdateCompanyRequest(
                "Tenant A Updated", null, null, null, null, null, null, null, null));

        assertThat(companyRepository.findById(companyA).orElseThrow().getName()).isEqualTo("Tenant A Updated");
        assertThat(companyRepository.findById(companyB).orElseThrow().getName()).isEqualTo("Tenant B");
    }

    @Test
    void branding_updatesOnlyRequestedCompany() {
        brandingService.updateBranding(companyA, new UpdateBrandingRequest(
                "#333333", null, null, null, null, null, null, null));

        assertThat(brandingRepository.findByCompanyId(companyA).orElseThrow().getPrimaryColor()).isEqualTo("#333333");
        assertThat(brandingRepository.findByCompanyId(companyB).orElseThrow().getPrimaryColor()).isEqualTo("#222222");
    }

    @Test
    void settings_updatesOnlyRequestedCompany() {
        settingsService.updateSettings(companyA, new UpdateSettingsRequest(
                "GBP", BigDecimal.ONE, null, null, null, null, null, null, null));

        assertThat(settingsRepository.findByCompanyId(companyA).orElseThrow().getDefaultCurrency()).isEqualTo("GBP");
        assertThat(settingsRepository.findByCompanyId(companyB).orElseThrow().getDefaultCurrency()).isEqualTo("USD");
    }

    @Test
    void services_areIsolatedForListUpdateDeleteAndReorder() {
        var serviceA = serviceCatalogService.createService(companyA,
                new CreateServiceRequest("Pintura", null, null, null, 0, true));
        var serviceB = serviceCatalogService.createService(companyB,
                new CreateServiceRequest("Pintura", null, null, null, 0, true));

        assertThat(serviceCatalogService.listServices(companyA)).extracting("id").containsExactly(serviceA.id());
        assertThat(serviceCatalogService.listServices(companyB)).extracting("id").containsExactly(serviceB.id());

        assertThatThrownBy(() -> serviceCatalogService.updateService(companyB, serviceA.id(),
                new UpdateServiceRequest("Cross tenant", null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> serviceCatalogService.reorder(companyB, serviceA.id(), 5))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> serviceCatalogService.deleteService(companyB, serviceA.id()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(serviceRepository.findById(serviceA.id()).orElseThrow().getName()).isEqualTo("Pintura");
    }

    @Test
    void gallery_isIsolatedForListUpdateDeleteFeatureReorderAndUpload() {
        var itemA = galleryService.createItem(companyA,
                new CreateGalleryRequest("Obra A", null, 0, false, true));
        var itemB = galleryService.createItem(companyB,
                new CreateGalleryRequest("Obra B", null, 0, false, true));

        assertThat(galleryService.listItems(companyA)).extracting("id").containsExactly(itemA.id());
        assertThat(galleryService.listItems(companyB)).extracting("id").containsExactly(itemB.id());

        assertThatThrownBy(() -> galleryService.updateItem(companyB, itemA.id(),
                new UpdateGalleryRequest("Cross tenant", null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> galleryService.feature(companyB, itemA.id(), true))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> galleryService.reorder(companyB, itemA.id(), 5))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> galleryService.uploadBeforeImage(companyB, itemA.id(), pngFile()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> galleryService.deleteItem(companyB, itemA.id()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(storageService, never()).store(any(), any());
        assertThat(galleryRepository.findById(itemA.id()).orElseThrow().getTitle()).isEqualTo("Obra A");
    }

    @Test
    void galleryUploadsUseAuthenticatedCompanyFolder() {
        var itemA = galleryService.createItem(companyA,
                new CreateGalleryRequest("Obra A", null, 0, false, true));
        when(storageService.store(eq("company/" + companyA + "/gallery"), any()))
                .thenReturn("/uploads/company/" + companyA + "/gallery/image.png");

        galleryService.uploadBeforeImage(companyA, itemA.id(), pngFile());

        verify(storageService).store(eq("company/" + companyA + "/gallery"), any());
    }

    @Test
    void customers_areIsolatedForListGetUpdateAndDelete() {
        var customerA = customerService.createCustomer(companyA,
                new CreateCustomerRequest("Cliente A", "a@example.com", null, null, null, null));
        var customerB = customerService.createCustomer(companyB,
                new CreateCustomerRequest("Cliente B", "b@example.com", null, null, null, null));

        assertThat(customerService.listCustomers(companyA)).extracting("id").containsExactly(customerA.id());
        assertThat(customerService.listCustomers(companyB)).extracting("id").containsExactly(customerB.id());

        assertThatThrownBy(() -> customerService.getCustomer(companyB, customerA.id()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> customerService.updateCustomer(companyB, customerA.id(),
                new UpdateCustomerRequest("Cross tenant", null, null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> customerService.deactivateCustomer(companyB, customerA.id()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> customerService.assertAssignable(companyB, customerA.id()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(customerRepository.findById(customerA.id()).orElseThrow().getName()).isEqualTo("Cliente A");
    }

    @Test
    void estimates_areIsolatedForListGetUpdateDeleteAndStatus() {
        var customerA = createCustomer(companyA, "Cliente A");
        var customerB = createCustomer(companyB, "Cliente B");
        var estimateA = estimateService.createEstimate(companyA, createEstimateRequest(customerA));
        var estimateB = estimateService.createEstimate(companyB, createEstimateRequest(customerB));

        assertThat(estimateService.listEstimates(companyA, null, null)).extracting("id").containsExactly(estimateA.id());
        assertThat(estimateService.listEstimates(companyB, null, null)).extracting("id").containsExactly(estimateB.id());

        assertThatThrownBy(() -> estimateService.getEstimate(companyB, estimateA.id()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> estimateService.updateEstimate(companyB, estimateA.id(),
                new UpdateEstimateRequest(null, "Cross tenant", null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> estimateService.changeStatus(companyB, estimateA.id(),
                new ChangeEstimateStatusRequest(EstimateStatus.SENT)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> estimateService.deleteEstimate(companyB, estimateA.id()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(estimateRepository.findById(estimateA.id()).orElseThrow().getTitle()).isEqualTo("Painting job");
    }

    @Test
    void estimateCreation_rejectsCustomerFromAnotherCompany() {
        var customerB = createCustomer(companyB, "Cliente B");

        assertThatThrownBy(() -> estimateService.createEstimate(companyA, createEstimateRequest(customerB)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void estimateCreation_rejectsServiceIdFromAnotherCompany() {
        var customerA = createCustomer(companyA, "Cliente A");
        var serviceB = serviceCatalogService.createService(companyB,
                new CreateServiceRequest("Pintura", null, null, null, 0, true));

        var item = new EstimateItemRequest(serviceB.id(), "Labor", java.math.BigDecimal.ONE, EstimateUnit.UNIT, java.math.BigDecimal.TEN, null);
        var request = new CreateEstimateRequest(customerA, "Painting job", null, null, null, null, null, null,
                null, null, java.util.List.of(item), java.util.List.of());

        assertThatThrownBy(() -> estimateService.createEstimate(companyA, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private java.util.UUID createCustomer(java.util.UUID companyId, String name) {
        return customerService.createCustomer(companyId,
                new CreateCustomerRequest(name, name.toLowerCase().replace(" ", ".") + "@example.com", null, null, null, null)).id();
    }

    private CreateEstimateRequest createEstimateRequest(java.util.UUID customerId) {
        return new CreateEstimateRequest(customerId, "Painting job", null, null, null, null, null, null,
                null, null, java.util.List.of(), java.util.List.of());
    }

    private Company createCompany(String name, String slug) {
        var company = new Company();
        company.setName(name);
        company.setSlug(slug);
        company.setEmail(slug + "@example.com");
        company.setCountry("PT");
        return companyRepository.save(company);
    }

    private void createBranding(UUID companyId, String primaryColor) {
        var branding = new Branding();
        branding.setCompanyId(companyId);
        branding.setPrimaryColor(primaryColor);
        brandingRepository.save(branding);
    }

    private void createSettings(UUID companyId, String currency) {
        var settings = new Settings();
        settings.setCompanyId(companyId);
        settings.setDefaultCurrency(currency);
        settingsRepository.save(settings);
    }

    private MockMultipartFile pngFile() {
        return new MockMultipartFile("file", "image.png", "image/png", new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00
        });
    }
}
