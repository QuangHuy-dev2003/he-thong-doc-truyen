package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.config.VnpayConfig;
import com.meobeo.truyen.domain.entity.PaymentTransaction;
import com.meobeo.truyen.domain.entity.TopupPackage;
import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.entity.UserWallet;
import com.meobeo.truyen.domain.entity.WalletTransaction;
import com.meobeo.truyen.domain.enums.TransactionType;
import com.meobeo.truyen.domain.request.topup.TopupPaymentRequest;
import com.meobeo.truyen.domain.response.topup.PaymentHistoryResponse;
import com.meobeo.truyen.domain.response.topup.PaymentResult;
import com.meobeo.truyen.domain.response.topup.TopupPaymentResponse;
import com.meobeo.truyen.exception.BadRequestException;
import com.meobeo.truyen.exception.ResourceNotFoundException;
import com.meobeo.truyen.repository.PaymentTransactionRepository;
import com.meobeo.truyen.repository.TopupPackageRepository;
import com.meobeo.truyen.repository.UserWalletRepository;
import com.meobeo.truyen.repository.WalletTransactionRepository;
import com.meobeo.truyen.service.interfaces.AsyncEmailService;
import com.meobeo.truyen.service.interfaces.VnpayService;
import com.meobeo.truyen.service.interfaces.VoucherService;
import com.meobeo.truyen.utils.CurrencyFormatUtil;
import com.meobeo.truyen.mapper.PaymentTransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Slf4j
public class VnpayServiceImpl implements VnpayService {

    private final VnpayConfig vnpayConfig;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final TopupPackageRepository topupPackageRepository;
    private final UserWalletRepository userWalletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final VoucherService voucherService;
    private final PaymentTransactionMapper paymentTransactionMapper;
    private final AsyncEmailService asyncEmailService;

    @Value("${app.wallet.url}")
    private String walletUrl;

    @Override
    @Transactional
    public TopupPaymentResponse createPaymentUrl(TopupPaymentRequest request, User user) {
        log.info("Tạo URL thanh toán VNPAY cho user {} với gói {}", user.getId(), request.getPackageId());

        // Validate TopupPackage
        TopupPackage topupPackage = topupPackageRepository.findByIdAndIsActiveTrue(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy gói nạp tiền với ID: " + request.getPackageId()));

        // Tính toán số tiền
        BigDecimal originalAmount = BigDecimal.valueOf(topupPackage.getAmount());
        BigDecimal discountAmount = calculateVoucherDiscount(request.getVoucherCode(), originalAmount, user.getId());
        BigDecimal finalAmount = originalAmount.subtract(discountAmount);

        // Tạo orderId
        String orderId = generateOrderId(user.getId(), request.getPackageId());

        // Kiểm tra orderId không trùng lặp
        if (paymentTransactionRepository.existsByOrderId(orderId)) {
            throw new BadRequestException("OrderId đã tồn tại, vui lòng thử lại");
        }

        // Tạo PaymentTransaction
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrderId(orderId);
        transaction.setAmount(finalAmount);
        transaction.setOriginalAmount(originalAmount);
        transaction.setDiscountAmount(discountAmount);
        transaction.setVoucherCode(request.getVoucherCode());
        transaction.setStatus(PaymentTransaction.PaymentStatus.PENDING);
        transaction.setUser(user);
        transaction.setTopupPackage(topupPackage);

        PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);

        // Tạo URL thanh toán VNPAY
        String paymentUrl = buildVnpayUrl(savedTransaction);

        // Cập nhật payment URL
        savedTransaction.setPaymentUrl(paymentUrl);
        paymentTransactionRepository.save(savedTransaction);

        // Tạo response
        TopupPaymentResponse response = new TopupPaymentResponse();
        response.setPaymentUrl(paymentUrl);
        response.setOrderId(orderId);
        response.setAmount(finalAmount);
        response.setOriginalAmount(originalAmount);
        response.setDiscountAmount(discountAmount);
        response.setVoucherCode(request.getVoucherCode());
        response.setPackageName(topupPackage.getName());
        response.setPackageId(topupPackage.getId());

        log.info("Đã tạo URL thanh toán thành công cho orderId: {}", orderId);
        return response;
    }

    @Override
    @Transactional
    public PaymentResult processPaymentCallback(Map<String, String> vnpayResponse) {
        log.info("Xử lý callback từ VNPAY: {}", vnpayResponse);

        PaymentResult result = new PaymentResult();

        try {
            // Validate chữ ký VNPAY
            if (!validateVnpaySignature(vnpayResponse)) {
                log.error("Chữ ký VNPAY không hợp lệ");
                result.setSuccess(false);
                result.setMessage("Chữ ký VNPAY không hợp lệ");
                return result;
            }

            String orderId = vnpayResponse.get("vnp_TxnRef");
            String vnpayTransactionId = vnpayResponse.get("vnp_TransactionNo");
            String responseCode = vnpayResponse.get("vnp_ResponseCode");
            String responseMessage = vnpayResponse.get("vnp_Message");
            String amount = vnpayResponse.get("vnp_Amount");

            // Tìm giao dịch
            PaymentTransaction transaction = paymentTransactionRepository.findByOrderId(orderId)
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Không tìm thấy giao dịch với orderId: " + orderId));

            // ======= Check giao dịch đã thành công trước đó chưa =======
            if (transaction.getStatus() == PaymentTransaction.PaymentStatus.SUCCESS) {
                log.warn("Giao dịch đã được xác nhận thành công trước đó!");
                result.setSuccess(true);
                result.setMessage("Giao dịch đã được xác nhận.");
                result.setTransaction(transaction);
                result.setOrderId(orderId);
                result.setStatus(transaction.getStatus().name());
                return result;
            }

            // ======= Check số tiền khớp không =======
            String amountDb = transaction.getAmount().multiply(BigDecimal.valueOf(100)).toBigInteger().toString();
            if (!amountDb.equals(amount)) {
                log.error("Số tiền nạp không khớp cho orderId: {}, DB: {}, Callback: {}", orderId, amountDb, amount);
                result.setSuccess(false);
                result.setMessage("Số tiền không khớp");
                return result;
            }

            // Cập nhật thông tin giao dịch
            transaction.setVnpayTransactionId(vnpayTransactionId);
            transaction.setVnpayResponseCode(responseCode);
            transaction.setVnpayResponseMessage(responseMessage);

            // Xử lý theo response code
            if ("00".equals(responseCode)) {
                // Thanh toán thành công
                transaction.setStatus(PaymentTransaction.PaymentStatus.SUCCESS);
                transaction.setPaidAt(LocalDateTime.now());

                // Cập nhật ví người dùng
                updateUserWallet(transaction.getUser(), transaction);

                // Tạo lịch sử giao dịch ví
                createWalletTransaction(transaction.getUser(), transaction);

                result.setSuccess(true);
                result.setMessage("Thanh toán thành công");
                log.info("Thanh toán thành công cho orderId: {}", orderId);

                // Gửi email thông báo thanh toán thành công bất đồng bộ
                sendTopupSuccessEmailAsync(transaction);

            } else {
                // Thanh toán thất bại
                transaction.setStatus(PaymentTransaction.PaymentStatus.FAILED);
                result.setSuccess(false);
                result.setMessage("Thanh toán thất bại: " + responseMessage);
                log.warn("Thanh toán thất bại cho orderId: {} - Code: {} - Message: {}",
                        orderId, responseCode, responseMessage);
            }

            paymentTransactionRepository.save(transaction);
            result.setTransaction(transaction);
            result.setOrderId(orderId);
            result.setStatus(transaction.getStatus().name());

        } catch (Exception e) {
            log.error("Lỗi xử lý callback VNPAY", e);
            result.setSuccess(false);
            result.setMessage("Lỗi xử lý callback: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResult checkPaymentStatus(String orderId, User user) {
        log.info("Kiểm tra trạng thái giao dịch orderId: {} cho user: {}", orderId, user.getId());

        PaymentTransaction transaction = paymentTransactionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch với orderId: " + orderId));

        // Kiểm tra quyền truy cập
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Không có quyền truy cập giao dịch này");
        }

        PaymentResult result = new PaymentResult();
        result.setSuccess(PaymentTransaction.PaymentStatus.SUCCESS.equals(transaction.getStatus()));
        result.setMessage("Trạng thái giao dịch: " + transaction.getStatus());
        result.setTransaction(transaction);
        result.setOrderId(orderId);
        result.setStatus(transaction.getStatus().name());

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentHistoryResponse getPaymentHistory(User user, Pageable pageable) {
        log.info("Lấy lịch sử giao dịch cho user: {}", user.getId());

        Page<PaymentTransaction> transactionPage = paymentTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        PaymentHistoryResponse response = new PaymentHistoryResponse();
        response.setTransactions(paymentTransactionMapper.toResponseList(transactionPage.getContent()));
        response.setTotalElements((int) transactionPage.getTotalElements());
        response.setTotalPages(transactionPage.getTotalPages());
        response.setCurrentPage(transactionPage.getNumber());
        response.setPageSize(transactionPage.getSize());

        return response;
    }

    @Override
    public String generateOrderId(Long userId, Long packageId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return String.format("TOPUP_%d_%s_%d", userId, timestamp, packageId);
    }

    @Override
    public BigDecimal calculateVoucherDiscount(String voucherCode, BigDecimal originalAmount, Long userId) {
        if (voucherCode == null || voucherCode.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            // Sử dụng VoucherService để tính toán giảm giá
            var discountResponse = voucherService.calculateDiscount(voucherCode, originalAmount);
            // Kiểm tra null và trả về giá trị mặc định nếu cần
            return discountResponse != null && discountResponse.getDiscountAmount() != null
                    ? discountResponse.getDiscountAmount()
                    : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Lỗi tính toán voucher {}: {}", voucherCode, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    @Override
    @Transactional
    public void updateUserWallet(User user, PaymentTransaction transaction) {
        log.info("Cập nhật ví cho user: {} với số tiền: {}", user.getId(), transaction.getAmount());

        UserWallet userWallet = userWalletRepository.findById(user.getId())
                .orElseGet(() -> {
                    UserWallet newWallet = new UserWallet();
                    newWallet.setUserId(user.getId());
                    newWallet.setUser(user);
                    newWallet.setBalance(0);
                    return userWalletRepository.save(newWallet);
                });

        int newBalance = userWallet.getBalance() + transaction.getAmount().intValue();
        userWallet.setBalance(newBalance);
        userWalletRepository.save(userWallet);

        log.info("Đã cập nhật ví thành công, số dư mới: {}", newBalance);
    }

    @Override
    @Transactional
    public void createWalletTransaction(User user, PaymentTransaction transaction) {
        log.info("Tạo lịch sử giao dịch ví cho user: {}", user.getId());

        WalletTransaction walletTransaction = new WalletTransaction();
        walletTransaction.setAmount(transaction.getAmount().intValue());
        walletTransaction.setCurrency(WalletTransaction.CurrencyType.VND);
        walletTransaction.setType(TransactionType.TOPUP);
        walletTransaction.setUser(user);

        String description = "Nạp tiền qua VNPAY - Gói: " + transaction.getTopupPackage().getName();
        if (transaction.getVoucherCode() != null) {
            description += " (Voucher: " + transaction.getVoucherCode() + ")";
        }
        walletTransaction.setDescription(description);

        walletTransactionRepository.save(walletTransaction);
        log.info("Đã tạo lịch sử giao dịch ví thành công");
    }

    @Override
    @Scheduled(fixedRate = 300000) // Chạy mỗi 5 phút
    @Transactional
    public void processExpiredTransactions() {
        log.info("Xử lý giao dịch hết hạn");

        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(vnpayConfig.getTimeoutMinutes());
        List<PaymentTransaction> expiredTransactions = paymentTransactionRepository
                .findExpiredPendingTransactions(expiredTime);

        for (PaymentTransaction transaction : expiredTransactions) {
            transaction.setStatus(PaymentTransaction.PaymentStatus.EXPIRED);
            paymentTransactionRepository.save(transaction);
            log.info("Đã đánh dấu giao dịch hết hạn: {}", transaction.getOrderId());
        }

        if (!expiredTransactions.isEmpty()) {
            log.info("Đã xử lý {} giao dịch hết hạn", expiredTransactions.size());
        }
    }

    /**
     * Tạo URL thanh toán VNPAY
     */
    private String buildVnpayUrl(PaymentTransaction transaction) {
        Map<String, String> vnpayParams = new HashMap<>();
        vnpayParams.put("vnp_Version", vnpayConfig.getVersion());
        vnpayParams.put("vnp_Command", vnpayConfig.getCommand());
        vnpayParams.put("vnp_TmnCode", vnpayConfig.getTmnCode());
        vnpayParams.put("vnp_Amount", transaction.getAmount().multiply(BigDecimal.valueOf(100)).longValue() + "");
        vnpayParams.put("vnp_CurrCode", vnpayConfig.getCurrency());
        vnpayParams.put("vnp_TxnRef", transaction.getOrderId());

        // Tạo thông tin đơn hàng đẹp hơn
        String orderInfo = "Nạp tiền - " + transaction.getTopupPackage().getName();
        // Chỉ bỏ dấu tiếng Việt, giữ lại space và dấu gạch ngang
        String orderInfoNoAccent = StringUtils.stripAccents(orderInfo)
                .replaceAll("[^a-zA-Z0-9\\s\\-]", ""); // Giữ lại chữ cái, số, space, dấu gạch ngang
        vnpayParams.put("vnp_OrderInfo", orderInfoNoAccent);
        vnpayParams.put("vnp_OrderType", vnpayConfig.getOrderType());
        vnpayParams.put("vnp_Locale", vnpayConfig.getLocale());
        vnpayParams.put("vnp_ReturnUrl", vnpayConfig.getReturnUrl());
        vnpayParams.put("vnp_IpAddr", "127.0.0.1");
        vnpayParams.put("vnp_CreateDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

        // Xóa param null/empty
        vnpayParams.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isEmpty());

        log.info("===> Params gửi sang VNPay:");
        vnpayParams.forEach((k, v) -> log.info("  {} = '{}'", k, v));

        // Build hash data và URL theo chuẩn VNPAY (giống mã tham khảo)
        List<String> fieldNames = new ArrayList<>(vnpayParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();

        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnpayParams.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data với URLEncoder
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                // Build query URL với URLEncoder
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        log.info("===> Hash data string: [{}]", hashData.toString());

        // Tạo chữ ký HMAC-SHA512
        String vnp_SecureHash = sha512(vnpayConfig.getHashSecret(), hashData.toString());
        log.info("===> Hash secret: [{}]", vnpayConfig.getHashSecret());
        log.info("===> Secure hash: [{}]", vnp_SecureHash);

        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String fullUrl = vnpayConfig.getUrl() + "?" + queryUrl;

        log.info("===> URL thanh toán VNPAY: [{}]", fullUrl);
        return fullUrl;
    }

    private String buildHashDataOldStyle(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String value = params.get(fieldName);
            if (value != null && !value.isEmpty()) {
                if (hashData.length() > 0) {
                    hashData.append("&");
                }
                hashData.append(fieldName).append("=").append(value);
            }
        }
        return hashData.toString();
    }

    private String sha512(String key, String data) {
        try {
            Mac sha512Hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            sha512Hmac.init(secretKeySpec);

            byte[] hmacBytes = sha512Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Chuyển đổi sang hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hmacBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception e) {
            log.error("Lỗi tạo HMAC SHA512: {}", e.getMessage(), e);
            throw new RuntimeException("Error creating HMAC SHA-512 hash", e);
        }
    }

    /**
     * Validate chữ ký VNPAY
     */
    private boolean validateVnpaySignature(Map<String, String> vnpayResponse) {
        try {
            String secureHash = vnpayResponse.get("vnp_SecureHash");
            if (secureHash == null) {
                return false;
            }

            // Tạo bản sao tham số và loại bỏ các key không tham gia chữ ký
            Map<String, String> paramsCopy = new HashMap<>(vnpayResponse);
            paramsCopy.remove("vnp_SecureHash");
            paramsCopy.remove("vnp_SecureHashType");

            // Sort key theo alphabet
            List<String> fieldNames = new ArrayList<>(paramsCopy.keySet());
            Collections.sort(fieldNames);

            // Build chuỗi hashData: key=value (value đã URL-encode) nối bằng '&'
            StringBuilder hashDataBuilder = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = paramsCopy.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashDataBuilder.append(fieldName);
                    hashDataBuilder.append('=');
                    hashDataBuilder.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                    if (itr.hasNext()) {
                        hashDataBuilder.append('&');
                    }
                }
            }

            String hashData = hashDataBuilder.toString();
            String expectedHash = sha512(vnpayConfig.getHashSecret(), hashData);
            log.info("===> Chuỗi hashData từ callback (encoded): [{}]", hashData);
            log.info("===> Expected hash (tự tính): [{}]", expectedHash);
            log.info("===> SecureHash từ VNPay gửi về: [{}]", secureHash);
            return secureHash.equalsIgnoreCase(expectedHash);
        } catch (Exception e) {
            log.error("Lỗi validate chữ ký VNPAY", e);
            return false;
        }
    }

    /**
     * Gửi email thông báo nạp tiền thành công bất đồng bộ
     */
    private void sendTopupSuccessEmailAsync(PaymentTransaction transaction) {
        try {
            User user = transaction.getUser();
            TopupPackage topupPackage = transaction.getTopupPackage();

            // Lấy số dư mới từ ví
            UserWallet userWallet = userWalletRepository.findById(user.getId()).orElse(null);
            String newBalance = userWallet != null ? CurrencyFormatUtil.formatVNDCurrency(userWallet.getBalance())
                    : "0 VNĐ";

            // Format thời gian
            String time = transaction.getPaidAt() != null
                    ? transaction.getPaidAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                    : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

            // Format số tiền nạp theo chuẩn Việt Nam
            String formattedAmount = CurrencyFormatUtil.formatVNDCurrency(transaction.getAmount());

            // Gửi email bất đồng bộ
            asyncEmailService.sendTopupSuccessEmailAsync(
                    user.getEmail(),
                    user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
                    topupPackage.getName(),
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
