package io.chicaodw.platform.gallery.domain;

import io.chicaodw.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "gallery_items")
@Getter
@Setter
@NoArgsConstructor
public class GalleryItem extends BaseEntity {

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "before_image_url", length = 2048)
    private String beforeImageUrl;

    @Column(name = "after_image_url", length = 2048)
    private String afterImageUrl;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean featured;

    @Column(nullable = false)
    private boolean active = true;
}
