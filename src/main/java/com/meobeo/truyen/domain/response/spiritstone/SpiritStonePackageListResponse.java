package com.meobeo.truyen.domain.response.spiritstone;

import lombok.Data;

import java.util.List;

@Data
public class SpiritStonePackageListResponse {

    private List<SpiritStonePackageResponse> packages;
    private long totalCount;
}
