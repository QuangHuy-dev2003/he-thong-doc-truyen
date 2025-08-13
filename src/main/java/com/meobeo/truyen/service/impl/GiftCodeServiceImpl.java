package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.*;
import com.meobeo.truyen.domain.enums.GiftCodeType;
import com.meobeo.truyen.domain.enums.TransactionType;
import com.meobeo.truyen.domain.request.giftcode.CreateGiftCodeRequest;
import com.meobeo.truyen.domain.request.giftcode.UpdateGiftCodeRequest;
import com.meobeo.truyen.domain.request.giftcode.UseGiftCodeRequest;
import com.meobeo.truyen.domain.response.giftcode.*;
import com.meobeo.truyen.exception.BadRequestException;
import com.meobeo.truyen.exception.ResourceNotFoundException;
import com.meobeo.truyen.repository.GiftCodeRepository;
import com.meobeo.truyen.repository.GiftCodeUsageRepository;
import com.meobeo.truyen.repository.UserRepository;
import com.meobeo.truyen.repository.UserWalletRepository;
import com.meobeo.truyen.repository.WalletTransactionRepository;
import com.meobeo.truyen.service.interfaces.GiftCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GiftCodeServiceImpl implements GiftCodeService {

    private final GiftCodeRepository giftCodeRepository;
    private final GiftCodeUsageRepository giftCodeUsageRepository;
    private final UserRepository userRepository;
    private final UserWalletRepository userWalletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    @Transactional
    public GiftCodeResponse createGiftCode(CreateGiftCodeRequest request) {
        log.info("Tạo gift code mới: {}", request.getCode());

        // Kiểm tra gift code đã tồn tại chưa
        if (giftCodeRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Mã quà tặng đã tồn tại");
        }

        // Validate theo loại gift code
        validateGiftCodeRequest(request);

        GiftCode giftCode = new GiftCode();
        giftCode.setCode(request.getCode());
        giftCode.setName(request.getName());
        giftCode.setAmount(request.getAmount());
        giftCode.setType(request.getType());
        giftCode.setMaxUsageCount(request.getMaxUsageCount());
        giftCode.setMaxUsersCount(request.getMaxUsersCount());
        giftCode.setMaxUsagePerUser(request.getMaxUsagePerUser());
        giftCode.setDescription(request.getDescription());
        giftCode.setIsActive(true);

        GiftCode savedGiftCode = giftCodeRepository.save(giftCode);
        log.info("Đã tạo gift code thành công: {}", savedGiftCode.getId());

        return mapToGiftCodeResponse(savedGiftCode);
    }

    @Override
    @Transactional
    public GiftCodeResponse updateGiftCode(Long id, UpdateGiftCodeRequest request) {
        log.info("Cập nhật gift code: {}", id);

        GiftCode giftCode = giftCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gift code"));

        // Validate theo loại gift code
        validateUpdateGiftCodeRequest(request);

        giftCode.setName(request.getName());
        giftCode.setAmount(request.getAmount());
        giftCode.setType(request.getType());
        giftCode.setMaxUsageCount(request.getMaxUsageCount());
        giftCode.setMaxUsersCount(request.getMaxUsersCount());
        giftCode.setMaxUsagePerUser(request.getMaxUsagePerUser());
        giftCode.setDescription(request.getDescription());

        if (request.getIsActive() != null) {
            giftCode.setIsActive(request.getIsActive());
        }

        GiftCode updatedGiftCode = giftCodeRepository.save(giftCode);
        log.info("Đã cập nhật gift code thành công: {}", updatedGiftCode.getId());

        return mapToGiftCodeResponse(updatedGiftCode);
    }

    @Override
    @Transactional
    public void deleteGiftCode(Long id) {
        log.info("Xóa gift code: {}", id);

        GiftCode giftCode = giftCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gift code"));

        // Kiểm tra xem gift code đã được sử dụng chưa
        long usageCount = giftCodeUsageRepository.countByGiftCodeId(id);
        if (usageCount > 0) {
            throw new BadRequestException("Không thể xóa gift code đã được sử dụng");
        }

        giftCodeRepository.delete(giftCode);
        log.info("Đã xóa gift code thành công: {}", id);
    }

    @Override
    public GiftCodeListResponse getAllActiveGiftCodes(Pageable pageable) {
        log.info("Lấy danh sách gift codes đang active");

        Page<GiftCode> giftCodePage = giftCodeRepository.findAll(pageable);

        List<GiftCodeResponse> giftCodeResponses = giftCodePage.getContent().stream()
                .map(this::mapToGiftCodeResponse)
                .collect(Collectors.toList());

        return new GiftCodeListResponse(
                giftCodeResponses,
                (int) giftCodePage.getTotalElements(),
                giftCodePage.getTotalPages(),
                giftCodePage.getNumber(),
                giftCodePage.getSize());
    }

    @Override
    public GiftCodeResponse getGiftCodeById(Long id) {
        log.info("Lấy gift code theo ID: {}", id);

        GiftCode giftCode = giftCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gift code"));

        return mapToGiftCodeResponse(giftCode);
    }

    @Override
    @Transactional
    public GiftCodeUsageResponse useGiftCode(UseGiftCodeRequest request, Long userId) {
        log.info("User {} sử dụng gift code: {}", userId, request.getCode());

        // Tìm gift code
        GiftCode giftCode = giftCodeRepository.findByCodeAndIsActiveTrue(request.getCode())
                .orElseThrow(() -> new BadRequestException("Mã quà tặng không hợp lệ hoặc đã bị vô hiệu hóa"));

        // Tìm user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // Kiểm tra tính hợp lệ của gift code
        validateGiftCodeUsage(giftCode, userId);

        // Tạo gift code usage record
        GiftCodeUsageId usageId = new GiftCodeUsageId(giftCode.getId(), userId);
        GiftCodeUsage usage = new GiftCodeUsage();
        usage.setId(usageId);
        usage.setGiftCode(giftCode);
        usage.setUser(user);
        usage.setUsedAt(LocalDateTime.now());

        giftCodeUsageRepository.save(usage);

        // Xử lý ví người dùng - tạo mới nếu chưa có
        UserWallet userWallet = userWalletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Tạo ví mới cho user: {}", userId);
                    UserWallet newWallet = new UserWallet();
                    newWallet.setUser(user);
                    newWallet.setBalance(0);
                    newWallet.setSpiritStones(0);
                    return newWallet;
                });

        // Cộng linh thạch vào ví
        int oldSpiritStones = userWallet.getSpiritStones();
        userWallet.setSpiritStones(oldSpiritStones + giftCode.getAmount());
        userWalletRepository.save(userWallet);

        // Tạo transaction record với currency SPIRIT_STONE
        WalletTransaction transaction = new WalletTransaction();
        transaction.setUser(user);
        transaction.setAmount(giftCode.getAmount());
        transaction.setType(TransactionType.GIFT_CODE);
        transaction.setCurrency(WalletTransaction.CurrencyType.SPIRIT_STONE);
        transaction.setDescription("Nhận linh thạch từ gift code: " + giftCode.getName());

        walletTransactionRepository.save(transaction);

        log.info("User {} đã sử dụng gift code {} thành công, nhận {} linh thạch. Số linh thạch cũ: {}, mới: {}",
                userId, request.getCode(), giftCode.getAmount(), oldSpiritStones, userWallet.getSpiritStones());

        return new GiftCodeUsageResponse(
                giftCode.getName(),
                giftCode.getAmount(),
                "Sử dụng gift code thành công! Bạn đã nhận " + giftCode.getAmount() + " linh thạch.",
                true);
    }

    @Override
    public GiftCodeUsageListResponse getGiftCodeUsageHistory(Long giftCodeId, Pageable pageable) {
        log.info("Lấy lịch sử sử dụng gift code: {}", giftCodeId);

        // Kiểm tra gift code tồn tại
        GiftCode giftCode = giftCodeRepository.findById(giftCodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gift code"));

        List<GiftCodeUsage> usages = giftCodeUsageRepository.findByGiftCodeIdOrderByUsedAtDesc(giftCodeId);

        List<GiftCodeUsageListResponse.GiftCodeUsageDetailResponse> usageDetails = usages.stream()
                .map(this::mapToUsageDetailResponse)
                .collect(Collectors.toList());

        return new GiftCodeUsageListResponse(
                usageDetails,
                usageDetails.size(),
                1,
                0,
                usageDetails.size());
    }

    @Override
    public boolean isValidGiftCode(String code, Long userId) {
        try {
            GiftCode giftCode = giftCodeRepository.findByCodeAndIsActiveTrue(code)
                    .orElse(null);

            if (giftCode == null) {
                return false;
            }

            validateGiftCodeUsage(giftCode, userId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void validateGiftCodeRequest(CreateGiftCodeRequest request) {
        switch (request.getType()) {
            case UNLIMITED:
                if (request.getMaxUsageCount() != null || request.getMaxUsersCount() != null) {
                    throw new BadRequestException("Gift code UNLIMITED không cần thiết lập giới hạn");
                }
                break;
            case LIMITED_USAGE:
                if (request.getMaxUsageCount() == null || request.getMaxUsageCount() <= 0) {
                    throw new BadRequestException("Gift code LIMITED_USAGE cần thiết lập maxUsageCount > 0");
                }
                break;
            case LIMITED_USERS:
                if (request.getMaxUsersCount() == null || request.getMaxUsersCount() <= 0) {
                    throw new BadRequestException("Gift code LIMITED_USERS cần thiết lập maxUsersCount > 0");
                }
                break;
        }
    }

    private void validateUpdateGiftCodeRequest(UpdateGiftCodeRequest request) {
        switch (request.getType()) {
            case UNLIMITED:
                if (request.getMaxUsageCount() != null || request.getMaxUsersCount() != null) {
                    throw new BadRequestException("Gift code UNLIMITED không cần thiết lập giới hạn");
                }
                break;
            case LIMITED_USAGE:
                if (request.getMaxUsageCount() == null || request.getMaxUsageCount() <= 0) {
                    throw new BadRequestException("Gift code LIMITED_USAGE cần thiết lập maxUsageCount > 0");
                }
                break;
            case LIMITED_USERS:
                if (request.getMaxUsersCount() == null || request.getMaxUsersCount() <= 0) {
                    throw new BadRequestException("Gift code LIMITED_USERS cần thiết lập maxUsersCount > 0");
                }
                break;
        }
    }

    private void validateGiftCodeUsage(GiftCode giftCode, Long userId) {
        // Kiểm tra gift code có active không
        if (!giftCode.getIsActive()) {
            throw new BadRequestException("Gift code đã bị vô hiệu hóa");
        }

        // Kiểm tra user đã sử dụng gift code này chưa
        long userUsageCount = giftCodeUsageRepository.countByGiftCodeIdAndUserId(giftCode.getId(), userId);
        if (userUsageCount >= giftCode.getMaxUsagePerUser()) {
            throw new BadRequestException("Bạn đã sử dụng hết lượt gift code này");
        }

        // Kiểm tra theo loại gift code
        switch (giftCode.getType()) {
            case LIMITED_USAGE:
                long totalUsageCount = giftCodeUsageRepository.countByGiftCodeId(giftCode.getId());
                if (totalUsageCount >= giftCode.getMaxUsageCount()) {
                    throw new BadRequestException("Gift code đã hết lượt sử dụng");
                }
                break;
            case LIMITED_USERS:
                long uniqueUserCount = giftCodeUsageRepository.findByGiftCodeIdOrderByUsedAtDesc(giftCode.getId())
                        .stream()
                        .map(usage -> usage.getUser().getId())
                        .distinct()
                        .count();
                if (uniqueUserCount >= giftCode.getMaxUsersCount()) {
                    throw new BadRequestException("Gift code đã đạt giới hạn số người sử dụng");
                }
                break;
            case UNLIMITED:
                // Không cần kiểm tra gì thêm
                break;
        }
    }

    private GiftCodeResponse mapToGiftCodeResponse(GiftCode giftCode) {
        long totalUsageCount = giftCodeUsageRepository.countByGiftCodeId(giftCode.getId());
        long uniqueUserCount = giftCodeUsageRepository.findByGiftCodeIdOrderByUsedAtDesc(giftCode.getId())
                .stream()
                .map(usage -> usage.getUser().getId())
                .distinct()
                .count();

        return new GiftCodeResponse(
                giftCode.getId(),
                giftCode.getCode(),
                giftCode.getName(),
                giftCode.getAmount(),
                giftCode.getType(),
                giftCode.getMaxUsageCount(),
                giftCode.getMaxUsersCount(),
                giftCode.getMaxUsagePerUser(),
                giftCode.getDescription(),
                giftCode.getIsActive(),
                giftCode.getCreatedAt(),
                totalUsageCount,
                uniqueUserCount);
    }

    private GiftCodeUsageListResponse.GiftCodeUsageDetailResponse mapToUsageDetailResponse(GiftCodeUsage usage) {
        return new GiftCodeUsageListResponse.GiftCodeUsageDetailResponse(
                usage.getGiftCode().getId(),
                usage.getGiftCode().getName(),
                usage.getGiftCode().getCode(),
                usage.getUser().getId(),
                usage.getUser().getUsername(),
                usage.getUser().getEmail(),
                usage.getGiftCode().getAmount(),
                usage.getUsedAt());
    }
}
