package io.chicaodw.platform.gallery.api.mapper;

import io.chicaodw.platform.gallery.api.dto.GalleryResponse;
import io.chicaodw.platform.gallery.api.dto.PublicGalleryResponse;
import io.chicaodw.platform.gallery.domain.GalleryItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GalleryMapper {

    GalleryResponse toResponse(GalleryItem item);

    PublicGalleryResponse toPublicResponse(GalleryItem item);
}
