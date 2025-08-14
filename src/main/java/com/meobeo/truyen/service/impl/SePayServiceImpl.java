package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.config.SePayConfig;
import com.meobeo.truyen.domain.entity.SePayTopupRequest;
import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.entity.UserWallet;
import com.meobeo.truyen.domain.entity.WalletTransaction;
import com.meobeo.truyen.domain.enums.TransactionType;
import com.meobeo.truyen.domain.repository.SePayTopupRequestRepository;
import com.meobeo.truyen.domain.request.topup.CreateSePayTopupRequest;
import com.meobeo.truyen.domain.request.topup.SePayWebhookRequest;
import com.meobeo.truyen.domain.response.topup.SePayTopupHistoryResponse;
import com.meobeo.truyen.domain.response.topup.SePayTopupResponse;
import com.meobeo.truyen.exception.BadRequestException;
import com.meobeo.truyen.exception.ResourceNotFoundException;
import com.meobeo.truyen.repository.UserWalletRepository;
import com.meobeo.truyen.repository.WalletTransactionRepository;
import com.meobeo.truyen.repository.TopupPackageRepository;
import com.meobeo.truyen.repository.UserRepository;
import com.meobeo.truyen.service.interfaces.SePayService;
import com.meobeo.truyen.service.interfaces.VoucherService;
import com.meobeo.truyen.service.interfaces.AsyncEmailService;
import com.meobeo.truyen.domain.mapper.SePayMapper;
import com.meobeo.truyen.utils.CurrencyFormatUtil;
import org.springframework.beans.factory.annotation.Value;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SePayServiceImpl implements SePayService {

    private final SePayConfig sePayConfig;
    private final SePayTopupRequestRepository sePayTopupRequestRepository;
    private final UserWalletRepository userWalletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final TopupPackageRepository topupPackageRepository;
    private final VoucherService voucherService;
    private final SePayMapper sePayMapper;
    private final AsyncEmailService asyncEmailService;
    private final UserRepository userRepository;

    @Value("${app.wallet.url}")
    private String walletUrl;

    @Override
    @Transactional
    public SePayTopupResponse createTopupRequest(CreateSePayTopupRequest request, User user) {
        log.info("Tạo yêu cầu nạp tiền SePay cho user {} với gói {}", user.getId(), request.getPackageId());

        // Validate TopupPackage
        var topupPackage = topupPackageRepository.findByIdAndIsActiveTrue(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy gói nạp tiền với ID: " + request.getPackageId()));

        // ======= KIỂM TRA VOUCHER TRƯỚC KHI TẠO PAYMENT URL =======
        BigDecimal originalAmount = BigDecimal.valueOf(topupPackage.getAmount());
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal finalAmount = originalAmount;

        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            try {
                // Kiểm tra voucher có hợp lệ không
                if (!voucherService.isValidVoucher(request.getVoucherCode(), user.getId())) {
                    throw new BadRequestException("Voucher không hợp lệ hoặc đã hết hạn");
                }

                // Tính toán giảm giá
                var discountResponse = voucherService.calculateDiscount(request.getVoucherCode(), originalAmount);

                if (!discountResponse.isValid()) {
                    throw new BadRequestException(discountResponse.getMessage());
                }

                discountAmount = discountResponse.getDiscountAmount();
                finalAmount = originalAmount.subtract(discountAmount);

                log.info("Voucher {} hợp lệ cho user {} với gói {}", request.getVoucherCode(), user.getId(),
                        topupPackage.getName());
            } catch (Exception e) {
                log.error("Lỗi kiểm tra voucher {}: {}", request.getVoucherCode(), e.getMessage());
                throw new BadRequestException("Không thể áp dụng voucher: " + e.getMessage());
            }
        }

        // Tạo mã nội dung chuyển khoản độc quyền
        String transferContent = generateTransferContent(user.getId());

        // Kiểm tra mã nội dung không trùng lặp
        if (sePayTopupRequestRepository.existsByTransferContent(transferContent)) {
            throw new BadRequestException("Mã nội dung chuyển khoản đã tồn tại, vui lòng thử lại");
        }

        // Tạo yêu cầu nạp tiền
        SePayTopupRequest topupRequest = new SePayTopupRequest();
        topupRequest.setUserId(user.getId());
        topupRequest.setAmount(finalAmount); // Số tiền thực tế phải chuyển
        topupRequest.setOriginalAmount(originalAmount); // Số tiền gốc của gói
        topupRequest.setDiscountAmount(discountAmount); // Số tiền giảm giá
        topupRequest.setVoucherCode(request.getVoucherCode());
        topupRequest.setTransferContent(transferContent);
        topupRequest.setStatus(SePayTopupRequest.TopupStatus.PENDING);

        SePayTopupRequest savedRequest = sePayTopupRequestRepository.save(topupRequest);

        // Tạo response sử dụng mapper
        SePayTopupResponse response = sePayMapper.toTopupResponse(savedRequest, topupPackage);

        log.info("Đã tạo yêu cầu nạp tiền thành công với ID: {}", savedRequest.getId());
        return response;
    }

    @Override
    @Transactional
    public boolean processWebhook(SePayWebhookRequest webhookRequest) {
        log.info("Xử lý webhook từ SePay: {}", webhookRequest);

        try {
            // Kiểm tra loại giao dịch (chỉ xử lý giao dịch vào)
            if (!"in".equalsIgnoreCase(webhookRequest.getTransferType())) {
                log.warn("Không xử lý giao dịch ra, transferType: {}", webhookRequest.getTransferType());
                return false;
            }

            // Kiểm tra giao dịch đã xử lý chưa
            if (sePayTopupRequestRepository.findBySepayTransactionId(webhookRequest.getId().toString()).isPresent()) {
                log.warn("Giao dịch đã được xử lý trước đó: {}", webhookRequest.getId());
                return true; // Trả về true vì đã xử lý thành công
            }

            // Trích xuất mã nội dung từ content
            String transferContent = extractTransferContent(webhookRequest.getContent());
            if (transferContent == null) {
                log.warn("Không tìm thấy mã nội dung hợp lệ trong content: {}", webhookRequest.getContent());
                return false;
            }

            log.info("🔍 Tìm kiếm yêu cầu nạp tiền với mã nội dung: {}", transferContent);

            // Tìm yêu cầu nạp tiền theo mã nội dung
            var optionalTopupRequest = sePayTopupRequestRepository.findByTransferContent(transferContent);

            if (optionalTopupRequest.isEmpty()) {
                // Debug: Lấy tất cả các yêu cầu nạp tiền pending để kiểm tra
                List<SePayTopupRequest> pendingRequests = sePayTopupRequestRepository
                        .findByStatus(SePayTopupRequest.TopupStatus.PENDING);
                log.warn("❌ Không tìm thấy yêu cầu nạp tiền với mã nội dung: {}", transferContent);
                log.warn("📋 Danh sách các yêu cầu nạp tiền PENDING trong DB:");
                for (SePayTopupRequest req : pendingRequests) {
                    log.warn("  - ID: {}, TransferContent: {}, Amount: {}, User: {}",
                            req.getId(), req.getTransferContent(), req.getAmount(), req.getUserId());
                }
                throw new ResourceNotFoundException(
                        "Không tìm thấy yêu cầu nạp tiền với mã nội dung: " + transferContent);
            }

            SePayTopupRequest topupRequest = optionalTopupRequest.get();
            log.info("✅ Tìm thấy yêu cầu nạp tiền: ID={}, User={}, Amount={}, TransferContent={}",
                    topupRequest.getId(), topupRequest.getUserId(), topupRequest.getAmount(),
                    topupRequest.getTransferContent());

            // Kiểm tra trạng thái yêu cầu
            if (topupRequest.getStatus() != SePayTopupRequest.TopupStatus.PENDING) {
                log.warn("Yêu cầu nạp tiền đã được xử lý, status: {}", topupRequest.getStatus());
                return false;
            }

            // Kiểm tra số tiền khớp
            log.info(
                    "Kiểm tra số tiền: expected={} (sau voucher), actual={} (đã chuyển), original={} (gói gốc), discount={} (giảm giá)",
                    topupRequest.getAmount(), webhookRequest.getTransferAmount(),
                    topupRequest.getOriginalAmount(), topupRequest.getDiscountAmount());

            if (topupRequest.getAmount().compareTo(webhookRequest.getTransferAmount()) != 0) {
                log.error("❌ Số tiền không khớp: expected={}, actual={}",
                        topupRequest.getAmount(), webhookRequest.getTransferAmount());
                return false;
            }

            log.info("✅ Số tiền khớp chính xác");

            // Cập nhật trạng thái yêu cầu nạp tiền
            topupRequest.setStatus(SePayTopupRequest.TopupStatus.SUCCESS);
            topupRequest.setSepayTransactionId(webhookRequest.getId().toString());
            topupRequest.setProcessedAt(LocalDateTime.now());
            sePayTopupRequestRepository.save(topupRequest);

            // Cộng tiền vào ví người dùng
            updateUserWallet(topupRequest);

            // Tạo lịch sử giao dịch
            createWalletTransaction(topupRequest);

            // Gửi email thông báo nạp tiền thành công
            sendTopupSuccessEmailAsync(topupRequest);

            log.info("Xử lý webhook thành công cho yêu cầu nạp tiền ID: {}", topupRequest.getId());
            return true;

        } catch (Exception e) {
            log.error("Lỗi xử lý webhook SePay", e);
            return false;
        }
    }

    @Override
    public SePayTopupHistoryResponse getTopupHistory(User user, Pageable pageable) {
        log.info("Lấy lịch sử nạp tiền SePay cho user {}", user.getId());

        Page<SePayTopupRequest> page = sePayTopupRequestRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        return sePayMapper.toHistoryResponse(page);
    }

    @Override
    @Transactional
    @Scheduled(fixedRate = 300000) // Chạy mỗi 5 phút
    public void processExpiredRequests() {
        log.info("Xử lý các yêu cầu nạp tiền hết hạn");

        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(sePayConfig.getTimeoutMinutes());
        List<SePayTopupRequest> expiredRequests = sePayTopupRequestRepository
                .findPendingRequestsBefore(expireTime);

        for (SePayTopupRequest request : expiredRequests) {
            request.setStatus(SePayTopupRequest.TopupStatus.EXPIRED);
            sePayTopupRequestRepository.save(request);
            log.info("Đã đánh dấu yêu cầu nạp tiền hết hạn: {}", request.getId());
        }

        if (!expiredRequests.isEmpty()) {
            log.info("Đã xử lý {} yêu cầu nạp tiền hết hạn", expiredRequests.size());
        }
    }

    @Override
    public SePayTopupRequest checkTopupStatus(Long requestId, Long userId) {
        log.info("Kiểm tra trạng thái yêu cầu nạp tiền ID: {} cho user: {}", requestId, userId);

        SePayTopupRequest topupRequest = sePayTopupRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy yêu cầu nạp tiền với ID: " + requestId));

        // Kiểm tra yêu cầu có thuộc về user không
        if (!topupRequest.getUserId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền truy cập yêu cầu nạp tiền này");
        }

        return topupRequest;
    }

    /**
     * Tạo mã nội dung chuyển khoản độc quyền
     * Format: NAP{userId}{randomString} (không có dấu gạch dưới)
     */
    private String generateTransferContent(Long userId) {
        String randomString = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("NAP%d%s", userId, randomString);
    }

    /**
     * Cập nhật ví người dùng
     */
    private void updateUserWallet(SePayTopupRequest topupRequest) {
        UserWallet wallet = userWalletRepository.findById(topupRequest.getUserId())
                .orElseGet(() -> {
                    UserWallet newWallet = new UserWallet();
                    newWallet.setUserId(topupRequest.getUserId());
                    newWallet.setBalance(0);
                    newWallet.setSpiritStones(0);
                    return newWallet;
                });

        // Cộng tiền vào ví (số tiền gốc của gói, không phải số tiền đã giảm giá)
        int oldBalance = wallet.getBalance();
        int addedAmount = topupRequest.getOriginalAmount().intValue();
        wallet.setBalance(oldBalance + addedAmount);
        userWalletRepository.save(wallet);

        log.info(
                "💰 Cộng tiền vào ví: user={}, balance cũ={}, cộng thêm={}, balance mới={} (gói gốc: {}, giảm giá: {})",
                topupRequest.getUserId(), oldBalance, addedAmount, wallet.getBalance(),
                topupRequest.getOriginalAmount(), topupRequest.getDiscountAmount());
    }

    /**
     * Tạo lịch sử giao dịch ví
     */
    private void createWalletTransaction(SePayTopupRequest topupRequest) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setAmount(topupRequest.getOriginalAmount().intValue()); // Lưu số tiền gốc vào lịch sử
        transaction.setCurrency(WalletTransaction.CurrencyType.VND);
        transaction.setType(TransactionType.TOPUP);

        String description = "Nạp tiền qua SePay - " + topupRequest.getTransferContent();
        if (topupRequest.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            description += String.format(" (Giảm giá: %s VND)", topupRequest.getDiscountAmount());
        }
        transaction.setDescription(description);

        User user = new User();
        user.setId(topupRequest.getUserId());
        transaction.setUser(user);

        walletTransactionRepository.save(transaction);

        log.info("Đã tạo lịch sử giao dịch ví cho user {} với số tiền {}",
                topupRequest.getUserId(), topupRequest.getOriginalAmount());
    }

    /**
     * Trích xuất mã nội dung chuyển khoản từ content của SePay
     * Format: "NHAN TU 091890889999 TRACE 554662 ND NAP7E566BF3C"
     * Cần trích xuất phần sau "ND "
     */
    private String extractTransferContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            log.warn("⚠️ Content rỗng hoặc null");
            return null;
        }

        log.info("🔍 Đang trích xuất mã nội dung từ content: '{}'", content);

        // Tìm phần sau "ND " (nội dung chuyển khoản)
        String[] parts = content.split("ND ");
        log.info("🔍 Split content theo 'ND ': {} parts", parts.length);

        if (parts.length >= 2) {
            String transferContent = parts[1].trim();
            log.info("✅ Trích xuất mã nội dung thành công: '{}'", transferContent);
            return transferContent;
        }

        log.warn("⚠️ Không thể trích xuất mã nội dung từ content: '{}'", content);
        return null;
    }

    /**
     * Gửi email thông báo nạp tiền thành công bất đồng bộ
     */
    private void sendTopupSuccessEmailAsync(SePayTopupRequest topupRequest) {
        try {
            // Lấy thông tin user từ DB
            User user = userRepository.findById(topupRequest.getUserId()).orElse(null);
            if (user == null) {
                log.warn("Không tìm thấy user với ID: {}", topupRequest.getUserId());
                return;
            }

            // Lấy số dư mới từ ví
            UserWallet userWallet = userWalletRepository.findById(topupRequest.getUserId()).orElse(null);
            String newBalance = userWallet != null ? CurrencyFormatUtil.formatVNDCurrency(userWallet.getBalance())
                    : "0 VNĐ";

            // Format thời gian
            String time = topupRequest.getProcessedAt() != null
                    ? topupRequest.getProcessedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                    : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

            // Format số tiền nạp theo chuẩn Việt Nam
            String formattedAmount = CurrencyFormatUtil.formatVNDCurrency(topupRequest.getOriginalAmount());

            // Tên gói nạp tiền (sử dụng số tiền gốc)
            String packageName = "Gói " + CurrencyFormatUtil.formatVNDCurrency(topupRequest.getOriginalAmount());

            // Gửi email bất đồng bộ
            asyncEmailService.sendTopupSuccessEmailAsync(
                    user.getEmail(),
                    user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
                    packageName,
                    formattedAmount,
                    newBalance,
                    time,
                    walletUrl);

            log.info("Đã khởi tạo gửi email thông báo nạp tiền thành công cho user: {}", user.getEmail());

        } catch (Exception e) {
            log.error("Lỗi khởi tạo gửi email thông báo nạp tiền thành công: {}", e.getMessage());
            // Không throw exception vì đây là async operation
        }
    }

}
