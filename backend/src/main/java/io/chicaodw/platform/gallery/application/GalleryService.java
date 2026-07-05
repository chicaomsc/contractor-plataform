package io.chicaodw.platform.gallery.application;

import io.chicaodw.platform.common.exception.ResourceNotFoundException;
import io.chicaodw.platform.gallery.api.dto.CreateGalleryRequest;
import io.chicaodw.platform.gallery.api.dto.GalleryResponse;
import io.chicaodw.platform.gallery.api.dto.PublicGalleryResponse;
import io.chicaodw.platform.gallery.api.dto.UpdateGalleryRequest;
import io.chicaodw.platform.gallery.api.mapper.GalleryMapper;
import io.chicaodw.platform.gallery.domain.GalleryItem;
import io.chicaodw.platform.gallery.infrastructure.persistence.GalleryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class GalleryService {

    private final GalleryRepository   galleryRepository;
    private final GalleryImageService galleryImageService;
    private final GalleryMapper       galleryMapper;

    // ── Admin operations ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<GalleryResponse> listItems(UUID companyId) {
        return galleryRepository.findByCompanyIdOrderByDisplayOrderAsc(companyId)
                                .stream().map(galleryMapper::toResponse).toList();
    }

    public GalleryResponse createItem(UUID companyId, CreateGalleryRequest request) {
        var item = new GalleryItem();
        item.setCompanyId(companyId);
        item.setTitle(request.title());
        item.setDescription(request.description());
        item.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        item.setFeatured(Boolean.TRUE.equals(request.featured()));
        item.setActive(request.active() == null || request.active());
        return galleryMapper.toResponse(galleryRepository.save(item));
    }

    public GalleryResponse updateItem(UUID companyId, UUID id, UpdateGalleryRequest request) {
        var item = findByIdAndCompany(id, companyId);

        if (request.title()        != null) item.setTitle(request.title());
        if (request.description()  != null) item.setDescription(request.description());
        if (request.displayOrder() != null) item.setDisplayOrder(request.displayOrder());
        if (request.featured()     != null) item.setFeatured(request.featured());
        if (request.active()       != null) item.setActive(request.active());

        return galleryMapper.toResponse(galleryRepository.save(item));
    }

    public void deleteItem(UUID companyId, UUID id) {
        var item = findByIdAndCompany(id, companyId);
        if (item.getBeforeImageUrl() != null) galleryImageService.deleteImage(item.getBeforeImageUrl());
        if (item.getAfterImageUrl()  != null) galleryImageService.deleteImage(item.getAfterImageUrl());
        galleryRepository.delete(item);
    }

    public GalleryResponse feature(UUID companyId, UUID id, boolean featured) {
        var item = findByIdAndCompany(id, companyId);
        item.setFeatured(featured);
        return galleryMapper.toResponse(galleryRepository.save(item));
    }

    public GalleryResponse reorder(UUID companyId, UUID id, int displayOrder) {
        var item = findByIdAndCompany(id, companyId);
        item.setDisplayOrder(displayOrder);
        return galleryMapper.toResponse(galleryRepository.save(item));
    }

    // ── Image management ──────────────────────────────────────────────────────

    public GalleryResponse uploadBeforeImage(UUID companyId, UUID id, MultipartFile file) {
        var item = findByIdAndCompany(id, companyId);
        if (item.getBeforeImageUrl() != null) galleryImageService.deleteImage(item.getBeforeImageUrl());
        item.setBeforeImageUrl(galleryImageService.storeImage(companyId, file));
        return galleryMapper.toResponse(galleryRepository.save(item));
    }

    public GalleryResponse uploadAfterImage(UUID companyId, UUID id, MultipartFile file) {
        var item = findByIdAndCompany(id, companyId);
        if (item.getAfterImageUrl() != null) galleryImageService.deleteImage(item.getAfterImageUrl());
        item.setAfterImageUrl(galleryImageService.storeImage(companyId, file));
        return galleryMapper.toResponse(galleryRepository.save(item));
    }

    public GalleryResponse deleteBeforeImage(UUID companyId, UUID id) {
        var item = findByIdAndCompany(id, companyId);
        if (item.getBeforeImageUrl() != null) {
            galleryImageService.deleteImage(item.getBeforeImageUrl());
            item.setBeforeImageUrl(null);
            galleryRepository.save(item);
        }
        return galleryMapper.toResponse(item);
    }

    public GalleryResponse deleteAfterImage(UUID companyId, UUID id) {
        var item = findByIdAndCompany(id, companyId);
        if (item.getAfterImageUrl() != null) {
            galleryImageService.deleteImage(item.getAfterImageUrl());
            item.setAfterImageUrl(null);
            galleryRepository.save(item);
        }
        return galleryMapper.toResponse(item);
    }

    // ── Public (no authentication) ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PublicGalleryResponse> listPublicItems(UUID companyId) {
        return galleryRepository.findByCompanyIdAndActiveTrueOrderByFeaturedDescDisplayOrderAsc(companyId)
                                .stream().map(galleryMapper::toPublicResponse).toList();
    }

    // ── Internal helper ───────────────────────────────────────────────────────

    private GalleryItem findByIdAndCompany(UUID id, UUID companyId) {
        return galleryRepository.findByIdAndCompanyId(id, companyId)
                                .orElseThrow(() -> new ResourceNotFoundException("GalleryItem", id));
    }
}
