package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.TopupPackage;
import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.entity.UserWallet;
import com.meobeo.truyen.domain.entity.WalletTransaction;
import com.meobeo.truyen.domain.enums.TransactionType;
import com.meobeo.truyen.domain.request.topup.CreateTopupPackageRequest;
import com.meobeo.truyen.domain.request.topup.TopupRequest;
import com.meobeo.truyen.domain.request.topup.UpdateTopupPackageRequest;
import com.meobeo.truyen.domain.response.topup.TopupPackageListResponse;
import com.meobeo.truyen.domain.response.topup.TopupPackageResponse;
import com.meobeo.truyen.exception.ResourceNotFoundException;
import com.meobeo.truyen.mapper.TopupPackageMapper;
import com.meobeo.truyen.repository.TopupPackageRepository;
import com.meobeo.truyen.repository.UserWalletRepository;
import com.meobeo.truyen.repository.WalletTransactionRepository;
import com.meobeo.truyen.service.TopupPackageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TopupPackageServiceImpl implements TopupPackageService {

    private final TopupPackageRepository topupPackageRepository;
    private final UserWalletRepository userWalletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final TopupPackageMapper topupPackageMapper;

    @Override
    @Transactional
    public TopupPackageResponse createTopupPackage(CreateTopupPackageRequest request) {
        log.info("Tạo gói nạp tiền mới: {}", request.getName());

        TopupPackage topupPackage = topupPackageMapper.toEntity(request);
        TopupPackage savedPackage = topupPackageRepository.save(topupPackage);

        log.info("Đã tạo gói nạp tiền thành công với ID: {}", savedPackage.getId());
        return topupPackageMapper.toResponse(savedPackage);
    }

    @Override
    @Transactional
    public TopupPackageResponse updateTopupPackage(Long id, UpdateTopupPackageRequest request) {
        log.info("Cập nhật gói nạp tiền với ID: {}", id);

        TopupPackage topupPackage = topupPackageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói nạp tiền với ID: " + id));

        topupPackageMapper.updateEntityFromRequest(topupPackage, request);
        TopupPackage updatedPackage = topupPackageRepository.save(topupPackage);

        log.info("Đã cập nhật gói nạp tiền thành công với ID: {}", id);
        return topupPackageMapper.toResponse(updatedPackage);
    }

    @Override
    @Transactional
    public void deleteTopupPackage(Long id) {
        log.info("Xóa gói nạp tiền với ID: {}", id);

        TopupPackage topupPackage = topupPackageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói nạp tiền với ID: " + id));

        topupPackage.setIsActive(false);
        topupPackageRepository.save(topupPackage);

        log.info("Đã xóa gói nạp tiền thành công với ID: {}", id);
    }

    @Override
    public TopupPackageListResponse getAllActivePackages() {
        log.info("Lấy danh sách tất cả gói nạp tiền đang hoạt động");

        List<TopupPackage> activePackages = topupPackageRepository.findByIsActiveTrue();
        List<TopupPackageResponse> packageResponses = topupPackageMapper.toResponseList(activePackages);

        TopupPackageListResponse response = new TopupPackageListResponse();
        response.setPackages(packageResponses);
        response.setTotalPackages(activePackages.size());

        log.info("Đã lấy {} gói nạp tiền đang hoạt động", activePackages.size());
        return response;
    }

    @Override
    public TopupPackageResponse getPackageById(Long id) {
        log.info("Lấy gói nạp tiền theo ID: {}", id);

        TopupPackage topupPackage = topupPackageRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói nạp tiền với ID: " + id));

        return topupPackageMapper.toResponse(topupPackage);
    }

    @Override
    @Transactional
    public void processTopup(TopupRequest request, User user) {
        log.info("Xử lý nạp tiền cho user ID: {} với gói ID: {}", user.getId(), request.getPackageId());

        // Lấy gói nạp tiền
        TopupPackage topupPackage = topupPackageRepository.findByIdAndIsActiveTrue(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy gói nạp tiền với ID: " + request.getPackageId()));

        // Lấy hoặc tạo ví của user
        UserWallet userWallet = userWalletRepository.findById(user.getId())
                .orElseGet(() -> {
                    UserWallet newWallet = new UserWallet();
                    newWallet.setUserId(user.getId());
                    newWallet.setUser(user);
                    newWallet.setBalance(0);
                    return userWalletRepository.save(newWallet);
                });

        // Cập nhật số dư
        int newBalance = userWallet.getBalance() + topupPackage.getAmount();
        userWallet.setBalance(newBalance);
        userWalletRepository.save(userWallet);

        // Tạo transaction
        WalletTransaction transaction = new WalletTransaction();
        transaction.setAmount(topupPackage.getAmount());
        transaction.setType(TransactionType.TOPUP);
        transaction.setDescription(
                "Nạp tiền gói: " + topupPackage.getName() + " (Mệnh giá: " + topupPackage.getAmount() + " VND)");
        transaction.setUser(user);
        walletTransactionRepository.save(transaction);

        log.info("Nạp tiền thành công cho user ID: {}. Số dư mới: {} VND", user.getId(), newBalance);
    }
}
