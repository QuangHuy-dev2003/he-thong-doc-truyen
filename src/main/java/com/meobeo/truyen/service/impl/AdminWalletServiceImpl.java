package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.entity.UserWallet;
import com.meobeo.truyen.domain.entity.WalletTransaction;
import com.meobeo.truyen.domain.enums.TransactionType;
import com.meobeo.truyen.domain.request.wallet.AdminWalletAdjustmentRequest;
import com.meobeo.truyen.domain.response.wallet.AdminWalletAdjustmentResponse;
import com.meobeo.truyen.domain.response.wallet.UserWalletInfoResponse;
import com.meobeo.truyen.domain.response.wallet.UserWalletListResponse;
import com.meobeo.truyen.domain.response.wallet.UserWalletTransactionListResponse;
import com.meobeo.truyen.domain.response.wallet.UserWalletTransactionResponse;
import com.meobeo.truyen.exception.BadRequestException;
import com.meobeo.truyen.exception.ResourceNotFoundException;
import com.meobeo.truyen.repository.UserRepository;
import com.meobeo.truyen.repository.UserWalletRepository;
import com.meobeo.truyen.repository.WalletTransactionRepository;
import com.meobeo.truyen.service.interfaces.AdminWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminWalletServiceImpl implements AdminWalletService {

    private final UserRepository userRepository;
    private final UserWalletRepository userWalletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    @Transactional
    public AdminWalletAdjustmentResponse adjustUserWallet(AdminWalletAdjustmentRequest request, String adminUsername) {
        log.info("Admin {} điều chỉnh ví cho user {}: {} {} {}",
                adminUsername, request.getUserId(), request.getAdjustmentType(),
                request.getAmount(), request.getCurrency());

        // Tìm user
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy người dùng với ID: " + request.getUserId()));

        // Lấy hoặc tạo ví
        UserWallet wallet = userWalletRepository.findByUserId(request.getUserId())
                .orElseGet(() -> {
                    UserWallet newWallet = new UserWallet();
                    newWallet.setUserId(request.getUserId());
                    newWallet.setUser(user);
                    newWallet.setBalance(0);
                    newWallet.setSpiritStones(0);
                    return userWalletRepository.save(newWallet);
                });

        // Lưu số dư cũ
        Integer oldBalance = wallet.getBalance();
        Integer oldSpiritStones = wallet.getSpiritStones();

        // Thực hiện điều chỉnh
        Integer adjustedAmount = request.getAmount();
        if (request.getAdjustmentType() == AdminWalletAdjustmentRequest.AdjustmentType.SUBTRACT) {
            adjustedAmount = -adjustedAmount;
        }

        // Cập nhật ví theo loại tiền tệ
        if (request.getCurrency() == WalletTransaction.CurrencyType.VND) {
            int newBalance = wallet.getBalance() + adjustedAmount;
            if (newBalance < 0) {
                throw new BadRequestException("Số dư tiền mặt không đủ để trừ. Số dư hiện tại: " + wallet.getBalance());
            }
            wallet.setBalance(newBalance);
        } else if (request.getCurrency() == WalletTransaction.CurrencyType.SPIRIT_STONE) {
            int newSpiritStones = wallet.getSpiritStones() + adjustedAmount;
            if (newSpiritStones < 0) {
                throw new BadRequestException(
                        "Số linh thạch không đủ để trừ. Số linh thạch hiện tại: " + wallet.getSpiritStones());
            }
            wallet.setSpiritStones(newSpiritStones);
        }

        userWalletRepository.save(wallet);

        // Tạo transaction record
        WalletTransaction transaction = new WalletTransaction();
        transaction.setUser(user);
        transaction.setAmount(adjustedAmount);
        transaction.setCurrency(request.getCurrency());
        transaction.setType(TransactionType.ADMIN_ADJUSTMENT);

        String description = request.getDescription();
        if (description == null || description.trim().isEmpty()) {
            description = String.format("Điều chỉnh bởi ADMIN %s - %s %d %s",
                    adminUsername,
                    request.getAdjustmentType() == AdminWalletAdjustmentRequest.AdjustmentType.ADD ? "Cộng" : "Trừ",
                    request.getAmount(),
                    request.getCurrency() == WalletTransaction.CurrencyType.VND ? "VND" : "Linh thạch");
        } else {
            description = String.format("Điều chỉnh bởi ADMIN %s: %s", adminUsername, description);
        }
        transaction.setDescription(description);

        walletTransactionRepository.save(transaction);

        // Tạo response
        AdminWalletAdjustmentResponse response = new AdminWalletAdjustmentResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setDisplayName(user.getDisplayName());
        response.setOldBalance(oldBalance);
        response.setNewBalance(wallet.getBalance());
        response.setOldSpiritStones(oldSpiritStones);
        response.setNewSpiritStones(wallet.getSpiritStones());
        response.setAdjustedAmount(adjustedAmount);
        response.setCurrency(request.getCurrency().name());
        response.setAdjustmentType(request.getAdjustmentType().name());
        response.setDescription(description);
        response.setAdjustedAt(LocalDateTime.now());
        response.setAdjustedBy(adminUsername);

        log.info("Điều chỉnh ví thành công - User: {}, Loại: {}, Số tiền: {}, Kết quả: Balance={}, SpiritStones={}",
                user.getUsername(), request.getAdjustmentType(), adjustedAmount,
                wallet.getBalance(), wallet.getSpiritStones());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public UserWalletInfoResponse getUserWalletInfo(Long userId) {
        log.info("Lấy thông tin ví cho user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        UserWallet wallet = userWalletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserWallet newWallet = new UserWallet();
                    newWallet.setUserId(userId);
                    newWallet.setBalance(0);
                    newWallet.setSpiritStones(0);
                    return newWallet;
                });

        UserWalletInfoResponse response = new UserWalletInfoResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setDisplayName(user.getDisplayName());
        response.setEmail(user.getEmail());
        response.setBalance(wallet.getBalance());
        response.setSpiritStones(wallet.getSpiritStones());
        response.setCreatedAt(user.getCreatedAt());

        // Lấy thời gian giao dịch cuối cùng
        Page<WalletTransaction> lastTransaction = walletTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, Pageable.ofSize(1));
        if (!lastTransaction.isEmpty()) {
            response.setLastTransactionAt(lastTransaction.getContent().get(0).getCreatedAt());
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public UserWalletTransactionListResponse getUserWalletTransactions(Long userId, Pageable pageable) {
        log.info("Lấy lịch sử giao dịch ví cho user ID: {}, page: {}, size: {}",
                userId, pageable.getPageNumber(), pageable.getPageSize());

        // Kiểm tra user tồn tại
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId);
        }

        Page<WalletTransaction> transactionPage = walletTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        UserWalletTransactionListResponse response = new UserWalletTransactionListResponse();
        response.setTransactions(transactionPage.getContent().stream()
                .map(this::mapToTransactionResponse)
                .toList());
        response.setPage(transactionPage.getNumber());
        response.setSize(transactionPage.getSize());
        response.setTotalElements(transactionPage.getTotalElements());
        response.setTotalPages(transactionPage.getTotalPages());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public UserWalletTransactionListResponse getAllWalletTransactions(Pageable pageable) {
        log.info("Lấy tất cả lịch sử giao dịch ví, page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<WalletTransaction> transactionPage = walletTransactionRepository
                .findAll(pageable);

        UserWalletTransactionListResponse response = new UserWalletTransactionListResponse();
        response.setTransactions(transactionPage.getContent().stream()
                .map(this::mapToTransactionResponse)
                .toList());
        response.setPage(transactionPage.getNumber());
        response.setSize(transactionPage.getSize());
        response.setTotalElements(transactionPage.getTotalElements());
        response.setTotalPages(transactionPage.getTotalPages());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public UserWalletListResponse getAllUserWallets(Pageable pageable) {
        log.info("Lấy danh sách tất cả ví người dùng, page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<User> userPage = userRepository.findAll(pageable);

        UserWalletListResponse response = new UserWalletListResponse();
        response.setWallets(userPage.getContent().stream()
                .map(user -> {
                    UserWallet wallet = userWalletRepository.findByUserId(user.getId())
                            .orElseGet(() -> {
                                UserWallet newWallet = new UserWallet();
                                newWallet.setUserId(user.getId());
                                newWallet.setBalance(0);
                                newWallet.setSpiritStones(0);
                                return newWallet;
                            });

                    UserWalletInfoResponse walletInfo = new UserWalletInfoResponse();
                    walletInfo.setUserId(user.getId());
                    walletInfo.setUsername(user.getUsername());
                    walletInfo.setDisplayName(user.getDisplayName());
                    walletInfo.setEmail(user.getEmail());
                    walletInfo.setBalance(wallet.getBalance());
                    walletInfo.setSpiritStones(wallet.getSpiritStones());
                    walletInfo.setCreatedAt(user.getCreatedAt());

                    // Lấy thời gian giao dịch cuối cùng
                    Page<WalletTransaction> lastTransaction = walletTransactionRepository
                            .findByUserIdOrderByCreatedAtDesc(user.getId(), Pageable.ofSize(1));
                    if (!lastTransaction.isEmpty()) {
                        walletInfo.setLastTransactionAt(lastTransaction.getContent().get(0).getCreatedAt());
                    }

                    return walletInfo;
                })
                .toList());
        response.setPage(userPage.getNumber());
        response.setSize(userPage.getSize());
        response.setTotalElements(userPage.getTotalElements());
        response.setTotalPages(userPage.getTotalPages());

        return response;
    }

    private UserWalletTransactionResponse mapToTransactionResponse(WalletTransaction transaction) {
        UserWalletTransactionResponse response = new UserWalletTransactionResponse();
        response.setId(transaction.getId());
        response.setUserId(transaction.getUser().getId());
        response.setUsername(transaction.getUser().getUsername());
        response.setDisplayName(transaction.getUser().getDisplayName());
        response.setAmount(transaction.getAmount());
        response.setCurrency(transaction.getCurrency().name());
        response.setType(transaction.getType().name());
        response.setDescription(transaction.getDescription());
        response.setCreatedAt(transaction.getCreatedAt());
        return response;
    }
}
