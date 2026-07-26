package io.chicaodw.platform.admin.api.dto;

import java.util.List;

/** Response of {@code GET /admin/companies/{id}} (DT-011A.7 §9). */
public record CompanyAdminDetailResponse(
        CompanyAdminDetail company,
        List<OwnerAdminResponse> owners
) {}
