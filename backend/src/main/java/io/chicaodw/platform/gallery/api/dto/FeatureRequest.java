package io.chicaodw.platform.gallery.api.dto;

import jakarta.validation.constraints.NotNull;

public record FeatureRequest(@NotNull Boolean featured) {}
