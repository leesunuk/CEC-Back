package com.backend.server.api.user.dto.equipment.equipment;

import java.util.List;

import org.springframework.data.domain.Page;

import com.backend.server.api.common.dto.PageableInfo;
import com.backend.server.api.user.dto.equipment.EquipmentRentalResponse;
import com.backend.server.model.entity.EquipmentModel;

public class EquipmentModelListResponse {
    private List<EquipmentRentalResponse> content;
    private PageableInfo pageable;

    public EquipmentModelListResponse(Page<EquipmentModel> page) {
        this.content = page.getContent().stream().map(EquipmentModelResponse::new).toList();
        this.pageable = new PageableInfo(page.getNumber(), page.getSize(), page.getTotalPages(), page.getTotalElements());
    }
}
