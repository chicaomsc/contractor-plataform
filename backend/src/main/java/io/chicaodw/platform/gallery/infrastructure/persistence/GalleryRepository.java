package io.chicaodw.platform.gallery.infrastructure.persistence;

import io.chicaodw.platform.gallery.domain.GalleryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GalleryRepository extends JpaRepository<GalleryItem, UUID> {

    List<GalleryItem> findByCompanyIdOrderByDisplayOrderAsc(UUID companyId);

    Optional<GalleryItem> findByIdAndCompanyId(UUID id, UUID companyId);

    /** Featured items first, then by displayOrder — used by the public API. */
    List<GalleryItem> findByCompanyIdAndActiveTrueOrderByFeaturedDescDisplayOrderAsc(UUID companyId);
}
