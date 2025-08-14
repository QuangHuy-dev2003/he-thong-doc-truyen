package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.entity.Voucher;
import com.meobeo.truyen.domain.entity.VoucherUsage;
import com.meobeo.truyen.domain.entity.VoucherUsageId;
import com.meobeo.truyen.domain.enums.VoucherStatus;
import com.meobeo.truyen.domain.enums.VoucherType;
import com.meobeo.truyen.repository.UserRepository;
import com.meobeo.truyen.domain.repository.VoucherRepository;
import com.meobeo.truyen.domain.repository.VoucherUsageRepository;
import com.meobeo.truyen.domain.request.voucher.ApplyVoucherRequest;
import com.meobeo.truyen.domain.request.voucher.CreateVoucherRequest;
import com.meobeo.truyen.domain.request.voucher.UpdateVoucherRequest;
import com.meobeo.truyen.domain.response.voucher.*;
import com.meobeo.truyen.exception.BadRequestException;
import com.meobeo.truyen.exception.ResourceNotFoundException;
import com.meobeo.truyen.service.interfaces.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final UserRepository userRepository;

    @Override
    public VoucherResponse createVoucher(CreateVoucherRequest request) {
        log.info("Tạo voucher mới với mã: {}", request.getCode());

        // Kiểm tra mã voucher đã tồn tại chưa
        if (voucherRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Mã voucher đã tồn tại");
        }

        // Kiểm tra thời gian hiệu lực
        if (request.getValidFrom().isAfter(request.getValidUntil())) {
            throw new BadRequestException("Thời gian bắt đầu phải trước thời gian kết thúc");
        }

        Voucher voucher = new Voucher();
        voucher.setCode(request.getCode());
        voucher.setName(request.getName());
        voucher.setType(request.getType());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMinAmount(request.getMinAmount());
        voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
        voucher.setStatus(VoucherStatus.ACTIVE);
        voucher.setMaxUsageCount(request.getMaxUsageCount());
        voucher.setMaxUsersCount(request.getMaxUsersCount());
        voucher.setMaxUsagePerUser(request.getMaxUsagePerUser());
        voucher.setValidFrom(request.getValidFrom());
        voucher.setValidUntil(request.getValidUntil());
        voucher.setDescription(request.getDescription());

        Voucher savedVoucher = voucherRepository.save(voucher);
        log.info("Đã tạo voucher thành công với ID: {}", savedVoucher.getId());

        return mapToVoucherResponse(savedVoucher);
    }

    @Override
    public VoucherResponse updateVoucher(Long id, UpdateVoucherRequest request) {
        log.info("Cập nhật voucher với ID: {}", id);

        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher với ID: " + id));

        // Kiểm tra thời gian hiệu lực
        if (request.getValidFrom().isAfter(request.getValidUntil())) {
            throw new BadRequestException("Thời gian bắt đầu phải trước thời gian kết thúc");
        }

        voucher.setName(request.getName());
        voucher.setType(request.getType());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMinAmount(request.getMinAmount());
        voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
        voucher.setStatus(request.getStatus());
        voucher.setMaxUsageCount(request.getMaxUsageCount());
        voucher.setMaxUsersCount(request.getMaxUsersCount());
        voucher.setMaxUsagePerUser(request.getMaxUsagePerUser());
        voucher.setValidFrom(request.getValidFrom());
        voucher.setValidUntil(request.getValidUntil());
        voucher.setDescription(request.getDescription());

        Voucher updatedVoucher = voucherRepository.save(voucher);
        log.info("Đã cập nhật voucher thành công với ID: {}", updatedVoucher.getId());

        return mapToVoucherResponse(updatedVoucher);
    }

    @Override
    public void deleteVoucher(Long id) {
        log.info("Xóa voucher với ID: {}", id);

        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher với ID: " + id));

        voucherRepository.delete(voucher);
        log.info("Đã xóa voucher thành công với ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherListResponse getAllVouchers(Pageable pageable) {
        log.info("Lấy danh sách voucher với phân trang");

        Page<Voucher> voucherPage = voucherRepository.findAll(pageable);

        VoucherListResponse response = new VoucherListResponse();
        response.setContent(voucherPage.getContent().stream()
                .map(this::mapToVoucherResponse)
                .collect(Collectors.toList()));
        response.setPage(voucherPage.getNumber());
        response.setSize(voucherPage.getSize());
        response.setTotalElements(voucherPage.getTotalElements());
        response.setTotalPages(voucherPage.getTotalPages());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse getVoucherById(Long id) {
        log.info("Lấy voucher theo ID: {}", id);

        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher với ID: " + id));

        return mapToVoucherResponse(voucher);
    }

    @Override
    public VoucherUsageResponse applyVoucher(ApplyVoucherRequest request, Long userId) {
        log.info("Áp dụng voucher {} cho user {}", request.getVoucherCode(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với ID: " + userId));

        Voucher voucher = getVoucherByCode(request.getVoucherCode());

        // Kiểm tra voucher có hợp lệ không
        if (!isValidVoucher(request.getVoucherCode(), userId)) {
            throw new BadRequestException("Voucher không hợp lệ hoặc đã hết hạn");
        }

        // Tính toán giảm giá
        BigDecimal discountAmount = calculateDiscountAmount(voucher, request.getAmount());
        BigDecimal finalAmount = request.getAmount().subtract(discountAmount);

        // Tạo voucher usage
        VoucherUsageId usageId = new VoucherUsageId(voucher.getId(), userId);
        VoucherUsage voucherUsage = new VoucherUsage();
        voucherUsage.setId(usageId);
        voucherUsage.setVoucher(voucher);
        voucherUsage.setUser(user);
        voucherUsage.setDiscountAmount(discountAmount);
        voucherUsage.setOriginalAmount(request.getAmount());
        voucherUsage.setUsedAt(LocalDateTime.now());

        voucherUsageRepository.save(voucherUsage);
        log.info("Đã áp dụng voucher thành công cho user {}", userId);

        return mapToVoucherUsageResponse(voucherUsage);
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherUsageListResponse getVoucherUsageHistory(Long voucherId, Pageable pageable) {
        log.info("Lấy lịch sử sử dụng voucher với ID: {}", voucherId);

        // Kiểm tra voucher tồn tại
        voucherRepository.findById(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher với ID: " + voucherId));

        Page<VoucherUsage> usagePage = voucherUsageRepository.findById_VoucherId(voucherId, pageable);

        VoucherUsageListResponse response = new VoucherUsageListResponse();
        response.setContent(usagePage.getContent().stream()
                .map(this::mapToVoucherUsageResponse)
                .collect(Collectors.toList()));
        response.setPage(usagePage.getNumber());
        response.setSize(usagePage.getSize());
        response.setTotalElements(usagePage.getTotalElements());
        response.setTotalPages(usagePage.getTotalPages());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isValidVoucher(String voucherCode, Long userId) {
        try {
            Voucher voucher = getVoucherByCode(voucherCode);
            log.info("Kiểm tra voucher: {} - Status: {}, ValidFrom: {}, ValidUntil: {}",
                    voucherCode, voucher.getStatus(), voucher.getValidFrom(), voucher.getValidUntil());

            // Kiểm tra trạng thái
            if (voucher.getStatus() != VoucherStatus.ACTIVE) {
                log.warn("Voucher {} không active, status: {}", voucherCode, voucher.getStatus());
                return false;
            }

            // Kiểm tra thời gian hiệu lực
            LocalDateTime now = LocalDateTime.now();
            log.info("Thời gian hiện tại: {}, ValidFrom: {}, ValidUntil: {}", now, voucher.getValidFrom(),
                    voucher.getValidUntil());

            if (now.isBefore(voucher.getValidFrom())) {
                log.warn("Voucher {} chưa đến thời gian hiệu lực", voucherCode);
                return false;
            }

            if (now.isAfter(voucher.getValidUntil())) {
                log.warn("Voucher {} đã hết hạn", voucherCode);
                return false;
            }

            // Kiểm tra số lần sử dụng tối đa
            if (voucher.getMaxUsageCount() != null) {
                long currentUsageCount = voucherUsageRepository.countById_VoucherId(voucher.getId());
                log.info("Voucher {} - MaxUsageCount: {}, CurrentUsageCount: {}",
                        voucherCode, voucher.getMaxUsageCount(), currentUsageCount);
                if (currentUsageCount >= voucher.getMaxUsageCount()) {
                    log.warn("Voucher {} đã đạt giới hạn sử dụng", voucherCode);
                    return false;
                }
            }

            // Kiểm tra số người dùng tối đa
            if (voucher.getMaxUsersCount() != null) {
                long currentUsersCount = voucherUsageRepository.countDistinctUsersByVoucherId(voucher.getId());
                log.info("Voucher {} - MaxUsersCount: {}, CurrentUsersCount: {}",
                        voucherCode, voucher.getMaxUsersCount(), currentUsersCount);
                if (currentUsersCount >= voucher.getMaxUsersCount()) {
                    log.warn("Voucher {} đã đạt giới hạn số người dùng", voucherCode);
                    return false;
                }
            }

            // Kiểm tra số lần sử dụng mỗi user (chỉ khi có userId)
            if (userId != null) {
                long userUsageCount = voucherUsageRepository.countById_VoucherIdAndId_UserId(voucher.getId(), userId);
                log.info("Voucher {} - User {} - MaxUsagePerUser: {}, UserUsageCount: {}",
                        voucherCode, userId, voucher.getMaxUsagePerUser(), userUsageCount);
                if (userUsageCount >= voucher.getMaxUsagePerUser()) {
                    log.warn("User {} đã sử dụng hết số lần cho phép của voucher {}", userId, voucherCode);
                    return false;
                }
            }

            log.info("Voucher {} hợp lệ", voucherCode);
            return true;
        } catch (Exception e) {
            log.warn("Lỗi khi kiểm tra voucher {}: {}", voucherCode, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DiscountCalculationResponse calculateDiscount(String voucherCode, BigDecimal amount) {
        log.info("Tính toán giảm giá cho voucher {} với số tiền {}", voucherCode, amount);

        DiscountCalculationResponse response = new DiscountCalculationResponse();
        response.setVoucherCode(voucherCode);
        response.setOriginalAmount(amount);

        try {
            Voucher voucher = getVoucherByCode(voucherCode);
            response.setVoucherName(voucher.getName());
            response.setType(voucher.getType());

            // Kiểm tra voucher có hợp lệ không
            if (!isValidVoucher(voucherCode, null)) {
                response.setValid(false);
                response.setMessage("Voucher không hợp lệ hoặc đã hết hạn");
                return response;
            }

            // Kiểm tra số tiền tối thiểu
            if (amount.compareTo(voucher.getMinAmount()) < 0) {
                response.setValid(false);
                response.setMessage("Số tiền nạp phải tối thiểu " + voucher.getMinAmount());
                return response;
            }

            // Tính toán giảm giá
            BigDecimal discountAmount = calculateDiscountAmount(voucher, amount);
            BigDecimal finalAmount = amount.subtract(discountAmount);
            BigDecimal discountPercentage = discountAmount.divide(amount, 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            response.setDiscountAmount(discountAmount);
            response.setFinalAmount(finalAmount);
            response.setDiscountPercentage(discountPercentage);
            response.setValid(true);
            response.setMessage("Voucher hợp lệ");

        } catch (Exception e) {
            response.setValid(false);
            response.setMessage("Voucher không tồn tại");
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Voucher getVoucherByCode(String code) {
        return voucherRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher với mã: " + code));
    }

    // Helper methods
    private VoucherResponse mapToVoucherResponse(Voucher voucher) {
        VoucherResponse response = new VoucherResponse();
        response.setId(voucher.getId());
        response.setCode(voucher.getCode());
        response.setName(voucher.getName());
        response.setType(voucher.getType());
        response.setDiscountValue(voucher.getDiscountValue());
        response.setMinAmount(voucher.getMinAmount());
        response.setMaxDiscountAmount(voucher.getMaxDiscountAmount());
        response.setStatus(voucher.getStatus());
        response.setMaxUsageCount(voucher.getMaxUsageCount());
        response.setMaxUsersCount(voucher.getMaxUsersCount());
        response.setMaxUsagePerUser(voucher.getMaxUsagePerUser());
        response.setValidFrom(voucher.getValidFrom());
        response.setValidUntil(voucher.getValidUntil());
        response.setDescription(voucher.getDescription());
        response.setCreatedAt(voucher.getCreatedAt());

        // Tính toán số liệu hiện tại
        response.setCurrentUsageCount(voucherUsageRepository.countById_VoucherId(voucher.getId()));
        response.setCurrentUsersCount(voucherUsageRepository.countDistinctUsersByVoucherId(voucher.getId()));

        return response;
    }

    private VoucherUsageResponse mapToVoucherUsageResponse(VoucherUsage voucherUsage) {
        VoucherUsageResponse response = new VoucherUsageResponse();
        response.setVoucherId(voucherUsage.getVoucher().getId());
        response.setVoucherCode(voucherUsage.getVoucher().getCode());
        response.setVoucherName(voucherUsage.getVoucher().getName());
        response.setUserId(voucherUsage.getUser().getId());
        response.setUserName(voucherUsage.getUser().getUsername());
        response.setOriginalAmount(voucherUsage.getOriginalAmount());
        response.setDiscountAmount(voucherUsage.getDiscountAmount());
        response.setFinalAmount(voucherUsage.getOriginalAmount().subtract(voucherUsage.getDiscountAmount()));
        response.setUsedAt(voucherUsage.getUsedAt());
        return response;
    }

    private BigDecimal calculateDiscountAmount(Voucher voucher, BigDecimal amount) {
        BigDecimal discountAmount;

        if (voucher.getType() == VoucherType.PERCENTAGE) {
            // Tính theo phần trăm
            discountAmount = amount.multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            // Kiểm tra giới hạn tối đa
            if (voucher.getMaxDiscountAmount() != null
                    && discountAmount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                discountAmount = voucher.getMaxDiscountAmount();
            }
        } else {
            // Tính theo số tiền cố định
            discountAmount = voucher.getDiscountValue();

            // Không được vượt quá số tiền gốc
            if (discountAmount.compareTo(amount) > 0) {
                discountAmount = amount;
            }
        }

        return discountAmount;
    }
}
