package com.meobeo.truyen.mapper;

import com.meobeo.truyen.domain.entity.SpiritStonePackage;
import com.meobeo.truyen.domain.request.spiritstone.CreateSpiritStonePackageRequest;
import com.meobeo.truyen.domain.request.spiritstone.UpdateSpiritStonePackageRequest;
import com.meobeo.truyen.domain.response.spiritstone.SpiritStonePackageResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SpiritStonePackageMapper {

    /**
     * Chuyển request thành entity
     */
    public SpiritStonePackage toEntity(CreateSpiritStonePackageRequest request) {
        SpiritStonePackage entity = new SpiritStonePackage();
        entity.setName(request.getName());
        entity.setSpiritStones(request.getSpiritStones());
        entity.setPrice(request.getPrice());
        entity.setBonusPercentage(request.getBonusPercentage());
        entity.setDescription(request.getDescription());
        entity.setIsActive(true);
        return entity;
    }

    /**
     * Cập nhật entity từ request
     */
    public void updateEntityFromRequest(SpiritStonePackage entity, UpdateSpiritStonePackageRequest request) {
        entity.setName(request.getName());
        entity.setSpiritStones(request.getSpiritStones());
        entity.setPrice(request.getPrice());
        entity.setBonusPercentage(request.getBonusPercentage());
        entity.setDescription(request.getDescription());
        entity.setIsActive(request.getIsActive());
    }

    /**
     * Chuyển entity thành response
     */
    public SpiritStonePackageResponse toResponse(SpiritStonePackage entity) {
        SpiritStonePackageResponse response = new SpiritStonePackageResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setSpiritStones(entity.getSpiritStones());
        response.setPrice(entity.getPrice());
        response.setBonusPercentage(entity.getBonusPercentage());
        response.setDescription(entity.getDescription());
        response.setIsActive(entity.getIsActive());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }

    /**
     * Chuyển danh sách entity thành danh sách response
     */
    public List<SpiritStonePackageResponse> toResponseList(List<SpiritStonePackage> entities) {
        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
