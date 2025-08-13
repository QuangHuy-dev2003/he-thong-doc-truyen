package com.meobeo.truyen.domain.response.topup;

import lombok.Data;

import java.util.List;

@Data
public class TopupPackageListResponse {
    private List<TopupPackageResponse> packages;
    private int totalPackages;
}
