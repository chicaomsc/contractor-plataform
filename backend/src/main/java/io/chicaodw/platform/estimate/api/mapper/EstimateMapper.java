package io.chicaodw.platform.estimate.api.mapper;

import io.chicaodw.platform.estimate.api.dto.EstimateItemResponse;
import io.chicaodw.platform.estimate.api.dto.EstimateResponse;
import io.chicaodw.platform.estimate.api.dto.EstimateSummaryResponse;
import io.chicaodw.platform.estimate.api.dto.MaterialResponse;
import io.chicaodw.platform.estimate.domain.Estimate;
import io.chicaodw.platform.estimate.domain.EstimateItem;
import io.chicaodw.platform.estimate.domain.Material;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EstimateMapper {

    EstimateResponse toResponse(Estimate estimate);

    EstimateSummaryResponse toSummaryResponse(Estimate estimate);

    EstimateItemResponse toItemResponse(EstimateItem item);

    MaterialResponse toMaterialResponse(Material material);
}
