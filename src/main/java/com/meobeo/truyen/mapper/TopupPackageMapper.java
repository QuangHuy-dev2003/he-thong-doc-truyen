package com.meobeo.truyen.mapper;

import com.meobeo.truyen.domain.entity.TopupPackage;
import com.meobeo.truyen.domain.request.topup.CreateTopupPackageRequest;
import com.meobeo.truyen.domain.request.topup.UpdateTopupPackageRequest;
import com.meobeo.truyen.domain.response.topup.TopupPackageResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TopupPackageMapper {

    /**
     * Chuyển đổi từ CreateTopupPackageRequest sang TopupPackage entity
     */
    public TopupPackage toEntity(CreateTopupPackageRequest request) {
        TopupPackage entity = new TopupPackage();
        entity.setName(request.getName());
        entity.setAmount(request.getAmount());
        entity.setPrice(request.getPrice());
        entity.setBonusPercentage(request.getBonusPercentage());
        entity.setDescription(request.getDescription());
        entity.setIsActive(true);
        return entity;
    }

    /**
     * Cập nhật TopupPackage entity từ UpdateTopupPackageRequest
     */
    public void updateEntityFromRequest(TopupPackage entity, UpdateTopupPackageRequest request) {
        entity.setName(request.getName());
        entity.setAmount(request.getAmount());
        entity.setPrice(request.getPrice());
        entity.setBonusPercentage(request.getBonusPercentage());
        entity.setDescription(request.getDescription());
        entity.setIsActive(request.getIsActive());
    }

    /**
     * Chuyển đổi từ TopupPackage entity sang TopupPackageResponse
     */
    public TopupPackageResponse toResponse(TopupPackage entity) {
        TopupPackageResponse response = new TopupPackageResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setAmount(entity.getAmount());
        response.setPrice(entity.getPrice());
        response.setBonusPercentage(entity.getBonusPercentage());
        response.setDescription(entity.getDescription());
        response.setIsActive(entity.getIsActive());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }

    /**
     * Chuyển đổi danh sách TopupPackage sang danh sách TopupPackageResponse
     */
    public List<TopupPackageResponse> toResponseList(List<TopupPackage> entities) {
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
