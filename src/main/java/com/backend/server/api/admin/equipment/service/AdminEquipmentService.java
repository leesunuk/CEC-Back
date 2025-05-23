package com.backend.server.api.admin.equipment.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.backend.server.api.common.notification.dto.CommonNotificationDto;
import com.backend.server.api.common.notification.service.CommonNotificationService;
import com.backend.server.model.entity.EquipmentCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.server.api.admin.equipment.dto.AdminManagerCandidatesResponse;
import com.backend.server.api.admin.equipment.dto.AdminEquipmentRentalRequest;
import com.backend.server.api.user.equipment.dto.equipment.EquipmentRentalRequest;
import com.backend.server.api.admin.equipment.dto.AdminEquipmentCreateRequest;
import com.backend.server.api.admin.equipment.dto.AdminEquipmentIdResponse;
import com.backend.server.api.admin.equipment.dto.AdminEquipmentIdsResponse;
import com.backend.server.api.admin.equipment.dto.AdminEquipmentListRequest;
import com.backend.server.api.admin.equipment.dto.AdminEquipmentListResponse;
import com.backend.server.api.admin.equipment.dto.AdminEquipmentResponse;
import com.backend.server.api.admin.equipment.dto.AdminEquipmentStatusUpdateRequest;
import com.backend.server.api.admin.equipment.dto.AdminEquipmentRentalActionResponse;
import com.backend.server.model.entity.Equipment;
import com.backend.server.model.entity.EquipmentModel;
import com.backend.server.model.entity.User;
import com.backend.server.model.entity.enums.Role;
import com.backend.server.model.entity.enums.Status;
import com.backend.server.model.repository.EquipmentCategoryRepository;
import com.backend.server.model.repository.EquipmentModelRepository;
import com.backend.server.model.repository.EquipmentRepository;
import com.backend.server.model.repository.EquipmentSpecification;
import com.backend.server.model.repository.UserRepository;
import com.backend.server.model.repository.EquipmentCartRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminEquipmentService {
    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentCategoryRepository equipmentCategoryRepository;
    private final EquipmentModelRepository equipmentModelRepository;
    private final CommonNotificationService notificationService;
    
    //어드민 유저 조회
    public List<AdminManagerCandidatesResponse> getAdminUsers() {
        List<User> adminUsers = userRepository.findByRoleIn(Role.ROLE_ADMIN,Role.ROLE_SUPER_ADMIN);
        return adminUsers.stream()
            .map(AdminManagerCandidatesResponse::new)
            .collect(Collectors.toList());
    }
    public String generateSerialNumber(AdminEquipmentCreateRequest request){
        EquipmentCategory category = equipmentCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("카테고리 없음"));

        EquipmentModel model = equipmentModelRepository.findById(request.getModelId())
                .orElseThrow(() -> new RuntimeException("모델 없음"));

        String equipmentCategoryName = category.getEnglishCode();
        String equipmentModelName = model.getEnglishCode();

        String prefixCategoryCode = equipmentCategoryName.substring(0, 3).toUpperCase();
        String prefixEquipmentModelCode = equipmentModelName.substring(0, 3).toUpperCase();
        Long modelCount = equipmentRepository.countByEquipmentModel_Id(request.getModelId());

        String modelCountString = modelCount < 10000
                ? String.format("%04d", modelCount)
                : modelCount.toString();

        return prefixCategoryCode + prefixEquipmentModelCode + modelCountString;
    }
    //장비생성
    public AdminEquipmentIdsResponse createEquipment(AdminEquipmentCreateRequest request) {
        EquipmentCategory category = equipmentCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("카테고리 없음"));

        EquipmentModel model = equipmentModelRepository.findById(request.getModelId())
                .orElseThrow(() -> new RuntimeException("모델 없음"));

        String equipmentCategoryName = category.getEnglishCode();
        String equipmentModelName = model.getEnglishCode();

        String prefixCategoryCode = equipmentCategoryName.substring(0, 3).toUpperCase();
        String prefixEquipmentModelCode = equipmentModelName.substring(0, 3).toUpperCase();

        Long modelCount = equipmentRepository.countByEquipmentModel_Id(request.getModelId());

        List<Long> savedEquipmentIds = new ArrayList<>();

        for (int i = 1; i <= request.getQuantity(); i++) {
            String modelCountString = modelCount < 10000
                    ? String.format("%04d", modelCount)
                    : modelCount.toString();

            String serialNumber = prefixCategoryCode + prefixEquipmentModelCode + modelCountString;

            Equipment equipment = request.toEntity(category, model, serialNumber);
            Equipment saved = equipmentRepository.save(equipment);
            savedEquipmentIds.add(saved.getId());

            modelCount++;
        }

        return new AdminEquipmentIdsResponse(savedEquipmentIds);
    }


    // 장비 업데이트
    public AdminEquipmentIdResponse updateEquipment(Long id, AdminEquipmentCreateRequest request) {
        Equipment equipment = equipmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("장비를 찾을 수 없습니다."));

        // toBuilder가 맞는거같아요
        Equipment updated = equipment.toBuilder()
            .imageUrl(request.getImageUrl())
            .managerId(request.getManagerId())
            .description(request.getDescription())
            .restrictionGrade(request.getRestrictionGrade())
            .build();

        equipmentRepository.save(updated); 

        return new AdminEquipmentIdResponse(updated.getId());
    }

    // 장비 삭제
    public AdminEquipmentIdResponse deleteEquipment(Long id) {
        equipmentRepository.deleteById(id);
        return new AdminEquipmentIdResponse(id);
    }

    //장비 리스트 어드민 조회
    public AdminEquipmentListResponse getEquipments(AdminEquipmentListRequest request) {
        Pageable pageable = EquipmentSpecification.getPageable(request);

        Specification<Equipment> spec = EquipmentSpecification.adminFilterEquipments(request);
        Page<Equipment> page = equipmentRepository.findAll(spec, pageable);
        
        List<AdminEquipmentResponse> responses = page.getContent().stream()
            .map(AdminEquipmentResponse::new)
            .toList();
        return new AdminEquipmentListResponse(responses, page);
    }

    //장비 단일 조회
    public AdminEquipmentResponse getEquipment(Long id) {
        Equipment equipment = equipmentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("장비를 찾을 수 없습니다."));

        return new AdminEquipmentResponse(equipment);
    }
    
    //장비 강제 상태 변경경
    @Transactional
    public void updateEquipmentStatus(Long equipmentId, AdminEquipmentStatusUpdateRequest request) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
            .orElseThrow(() -> new RuntimeException("장비를 찾을 수 없습니다."));
        
        Equipment updated = equipment.toBuilder()
            .status(request.getStatus())
            .build();
        
        equipmentRepository.save(updated);
        
    }
    
    //대여 요청 다중 승인
    @Transactional
    public void approveRentalRequests(List<Long> equipmentIds) {
        for (Long equipmentId : equipmentIds) {
            Equipment equipment = equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new RuntimeException("장비를 찾을 수 없습니다. ID: " + equipmentId));

            if (equipment.getStatus() != Status.RENTAL_PENDING) {
                throw new IllegalStateException("대여 요청 상태인 장비만 승인할 수 있습니다. ID: " + equipmentId);
            }

            // 상태 업데이트
            Equipment updated = equipment.toBuilder()
                    .status(Status.IN_USE)
                    .build();

            equipmentRepository.save(updated);

            // 대여자 정보 추출
            Long userId = equipment.getRenter().getId(); // 또는 getUser().getId()

            // 알림 전송
            CommonNotificationDto dto = CommonNotificationDto.builder()
                    .category("장비 대여 승인")
                    .title("장비 대여가 승인되었습니다.")
                    .message("요청하신 장비 [" + equipment.getEquipmentModel().getName() + "]의 대여가 승인되었습니다.")
                    .link("/equipment/" + equipmentId)
                    .build();

            notificationService.createNotification(dto, userId);
        }
    }


    //대여요청 거절절
    @Transactional
    public void rejectRentalRequests(List<Long> equipmentIds) {
        for (Long equipmentId : equipmentIds) {
            Equipment equipment = equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new RuntimeException("장비를 찾을 수 없습니다. ID: " + equipmentId));

            if (equipment.getStatus() != Status.RENTAL_PENDING) {
                throw new IllegalStateException("대여 요청 상태인 장비만 거절할 수 있습니다. ID: " + equipmentId);
            }

            Long userId = equipment.getRenter().getId(); // null 설정 전
            Equipment updated = equipment.toBuilder()
                    .status(Status.AVAILABLE)
                    .renter(null)
                    .startRentDate(null)
                    .endRentDate(null)
                    .build();

            equipmentRepository.save(updated);

            CommonNotificationDto dto = CommonNotificationDto.builder()
                    .category("장비 대여 거절")
                    .title("장비 대여가 거절되었습니다.")
                    .message("요청하신 장비 [" + equipment.getEquipmentModel().getName() + "]의 대여가 거절되었습니다.")
                    .link(null)
                    .build();

            notificationService.createNotification(dto, userId);
        }
    }

    @Transactional
    public void processReturnRequests(List<Long> equipmentIds) {
        for (Long equipmentId : equipmentIds) {
            Equipment equipment = equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new RuntimeException("장비를 찾을 수 없습니다. ID: " + equipmentId));

            if (Status.RETURN_PENDING != equipment.getStatus() && Status.IN_USE != equipment.getStatus()) {
                throw new IllegalStateException("반납 요청 상태 또는 대여중인 장비만 반납 처리할 수 있습니다. ID: " + equipmentId);
            }

            Equipment updated = equipment.toBuilder()
                    .status(Status.AVAILABLE)
                    .renter(null)
                    .startRentDate(null)
                    .endRentDate(null)
                    .build();

            equipmentRepository.save(updated);
        }
    }

    @Transactional
    public void markEquipmentsAsBroken(List<Long> equipmentIds, String description) {
        for (Long equipmentId : equipmentIds) {
            Equipment equipment = equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new RuntimeException("장비를 찾을 수 없습니다. ID: " + equipmentId));

            Long currentBrokenCount = equipment.getBrokenCount();

            Equipment updated = equipment.toBuilder()
                    .status(Status.BROKEN)
                    .description(description != null ?
                            equipment.getDescription() + "\n[" + LocalDateTime.now() + "] 고장/파손: " + description :
                            equipment.getDescription())
                    .brokenCount(currentBrokenCount != null ? currentBrokenCount + 1L : 1L)
                    .build();

            equipmentRepository.save(updated);
        }
    }

    @Transactional
    public void repairEquipments(List<Long> equipmentIds, String repairNote) {
        for (Long equipmentId : equipmentIds) {
            Equipment equipment = equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new RuntimeException("장비를 찾을 수 없습니다. ID: " + equipmentId));

            if (Status.BROKEN != equipment.getStatus()) {
                throw new IllegalStateException("고장/파손 상태인 장비만 복구할 수 있습니다. ID: " + equipmentId);
            }

            Equipment updated = equipment.toBuilder()
                    .status(Status.AVAILABLE)
                    .description(repairNote != null ?
                            equipment.getDescription() + "\n[" + LocalDateTime.now() + "] 복구: " + repairNote :
                            equipment.getDescription())
                    .build();

            equipmentRepository.save(updated);
        }
    }

    @Transactional
    public void extendRentalPeriods(List<Long> equipmentIds, LocalDateTime newEndDate) {
        if (newEndDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("연장 날짜는 현재 시간보다 이후여야 합니다.");
        }

        for (Long equipmentId : equipmentIds) {
            Equipment equipment = equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new RuntimeException("장비를 찾을 수 없습니다. ID: " + equipmentId));

            if (Status.IN_USE != equipment.getStatus()) {
                throw new IllegalStateException("대여 중인 장비만 기간을 연장할 수 있습니다. ID: " + equipmentId);
            }

            Equipment updated = equipment.toBuilder()
                    .endRentDate(newEndDate)
                    .build();

            equipmentRepository.save(updated);
        }
    }

    @Transactional
    public void forceReturnEquipments(List<Long> equipmentIds) {
        for (Long equipmentId : equipmentIds) {
            Equipment equipment = equipmentRepository.findById(equipmentId)
                    .orElseThrow(() -> new RuntimeException("장비를 찾을 수 없습니다. ID: " + equipmentId));

            if (Status.IN_USE != equipment.getStatus()) {
                throw new IllegalStateException("대여 중인 장비만 강제 회수할 수 있습니다. ID: " + equipmentId);
            }

            Equipment updated = equipment.toBuilder()
                    .status(Status.AVAILABLE)
                    .renter(null)
                    .startRentDate(null)
                    .endRentDate(null)
                    .build();

            equipmentRepository.save(updated);
        }
    }


    /**
     * 대여 중인 장비 일괄 조회
     */
    public AdminEquipmentListResponse getRentedEquipments() {
        List<Equipment> rentedEquipments = equipmentRepository.findByStatus(Status.IN_USE);
        
        List<AdminEquipmentResponse> responses = rentedEquipments.stream()
            .map(AdminEquipmentResponse::new)
            .toList();
        
        return new AdminEquipmentListResponse(responses, rentedEquipments.size());
    }
    
    
}
