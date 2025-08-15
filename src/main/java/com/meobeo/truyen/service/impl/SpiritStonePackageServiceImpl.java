package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.domain.entity.SpiritStonePackage;
import com.meobeo.truyen.domain.entity.User;
import com.meobeo.truyen.domain.entity.UserWallet;
import com.meobeo.truyen.domain.entity.WalletTransaction;
import com.meobeo.truyen.domain.enums.TransactionType;
import com.meobeo.truyen.domain.repository.SpiritStonePackageRepository;
import com.meobeo.truyen.domain.request.spiritstone.ExchangeSpiritStoneByAmountRequest;
import com.meobeo.truyen.domain.request.spiritstone.ExchangeSpiritStoneByPackageRequest;
import com.meobeo.truyen.domain.request.spiritstone.CreateSpiritStonePackageRequest;
import com.meobeo.truyen.domain.request.spiritstone.UpdateSpiritStonePackageRequest;
import com.meobeo.truyen.domain.response.spiritstone.ExchangeHistoryListResponse;
import com.meobeo.truyen.domain.response.spiritstone.SpiritStonePackageListResponse;
import com.meobeo.truyen.domain.response.spiritstone.SpiritStonePackageResponse;
import com.meobeo.truyen.domain.response.spiritstone.WalletBalanceResponse;
import com.meobeo.truyen.exception.InsufficientBalanceException;
import com.meobeo.truyen.exception.PackageNotFoundException;
import com.meobeo.truyen.mapper.ExchangeHistoryMapper;
import com.meobeo.truyen.mapper.SpiritStonePackageMapper;
import com.meobeo.truyen.repository.UserWalletRepository;
import com.meobeo.truyen.repository.WalletTransactionRepository;
import com.meobeo.truyen.service.interfaces.SpiritStonePackageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.meobeo.truyen.domain.response.spiritstone.ExchangeHistoryResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpiritStonePackageServiceImpl implements SpiritStonePackageService {

        private final SpiritStonePackageRepository spiritStonePackageRepository;
        private final UserWalletRepository userWalletRepository;
        private final WalletTransactionRepository walletTransactionRepository;
        private final SpiritStonePackageMapper spiritStonePackageMapper;
        private final ExchangeHistoryMapper exchangeHistoryMapper;

        // Tỷ giá cố định: 50 VND = 1 linh thạch
        private static final double EXCHANGE_RATE = 50.0;

        @Override
        @Transactional
        public SpiritStonePackageResponse createSpiritStonePackage(CreateSpiritStonePackageRequest request) {
                log.info("Tạo gói đổi linh thạch mới: {}", request.getName());

                SpiritStonePackage entity = spiritStonePackageMapper.toEntity(request);
                SpiritStonePackage savedEntity = spiritStonePackageRepository.save(entity);

                log.info("Đã tạo gói đổi linh thạch thành công với ID: {}", savedEntity.getId());
                return spiritStonePackageMapper.toResponse(savedEntity);
        }

        @Override
        @Transactional
        public SpiritStonePackageResponse updateSpiritStonePackage(Long id, UpdateSpiritStonePackageRequest request) {
                log.info("Cập nhật gói đổi linh thạch với ID: {}", id);

                SpiritStonePackage entity = spiritStonePackageRepository.findById(id)
                                .orElseThrow(() -> new PackageNotFoundException(
                                                "Không tìm thấy gói đổi linh thạch với ID: " + id));

                spiritStonePackageMapper.updateEntityFromRequest(entity, request);
                SpiritStonePackage savedEntity = spiritStonePackageRepository.save(entity);

                log.info("Đã cập nhật gói đổi linh thạch thành công với ID: {}", id);
                return spiritStonePackageMapper.toResponse(savedEntity);
        }

        @Override
        @Transactional
        public void deleteSpiritStonePackage(Long id) {
                log.info("Xóa gói đổi linh thạch với ID: {}", id);

                SpiritStonePackage entity = spiritStonePackageRepository.findById(id)
                                .orElseThrow(() -> new PackageNotFoundException(
                                                "Không tìm thấy gói đổi linh thạch với ID: " + id));

                entity.setIsActive(false);
                spiritStonePackageRepository.save(entity);

                log.info("Đã xóa gói đổi linh thạch thành công với ID: {}", id);
        }

        @Override
        public SpiritStonePackageListResponse getAllActivePackages() {
                log.info("Lấy tất cả gói đổi linh thạch đang hoạt động");

                List<SpiritStonePackage> packages = spiritStonePackageRepository.findByIsActiveTrue();
                List<SpiritStonePackageResponse> responses = spiritStonePackageMapper.toResponseList(packages);

                SpiritStonePackageListResponse result = new SpiritStonePackageListResponse();
                result.setPackages(responses);
                result.setTotalCount(packages.size());

                return result;
        }

        @Override
        public SpiritStonePackageResponse getPackageById(Long id) {
                log.info("Lấy chi tiết gói đổi linh thạch với ID: {}", id);

                SpiritStonePackage entity = spiritStonePackageRepository.findByIdAndIsActiveTrue(id)
                                .orElseThrow(() -> new PackageNotFoundException(
                                                "Không tìm thấy gói đổi linh thạch với ID: " + id));

                return spiritStonePackageMapper.toResponse(entity);
        }

        @Override
        @Transactional
        public WalletBalanceResponse exchangeSpiritStoneByPackage(ExchangeSpiritStoneByPackageRequest request,
                        Long userId) {
                log.info("Thực hiện đổi linh thạch theo gói cho user ID: {}, package ID: {}", userId,
                                request.getPackageId());

                // Kiểm tra số dư tiền mặt
                UserWallet userWallet = userWalletRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví của người dùng"));

                // Đổi theo gói
                SpiritStonePackage packageEntity = spiritStonePackageRepository
                                .findByIdAndIsActiveTrue(request.getPackageId())
                                .orElseThrow(() -> new PackageNotFoundException(
                                                "Không tìm thấy gói đổi linh thạch với ID: " + request.getPackageId()));

                int amountToSpend = packageEntity.getPrice();
                int spiritStonesReceived = (int) (packageEntity.getSpiritStones()
                                * (1 + packageEntity.getBonusPercentage()));
                String description = "Đổi linh thạch - Gói: " + packageEntity.getName() +
                                " (Giá: " + packageEntity.getPrice() + " VND, Nhận: " + spiritStonesReceived
                                + " linh thạch)";

                // Kiểm tra số dư đủ không
                if (userWallet.getBalance() < amountToSpend) {
                        throw new InsufficientBalanceException("Số dư tiền mặt không đủ để đổi linh thạch. Cần: "
                                        + amountToSpend + " VND, Hiện có: " + userWallet.getBalance() + " VND");
                }

                // Thực hiện giao dịch
                performExchange(userWallet, userId, amountToSpend, spiritStonesReceived, description);

                // Tạo response
                WalletBalanceResponse response = new WalletBalanceResponse();
                response.setBalance(userWallet.getBalance());
                response.setSpiritStones(userWallet.getSpiritStones());
                response.setAmountSpent(amountToSpend);
                response.setSpiritStonesReceived(spiritStonesReceived);
                response.setDescription(description);

                log.info("Đổi linh thạch theo gói thành công. User ID: {}, Chi: {} VND, Nhận: {} linh thạch",
                                userId, amountToSpend, spiritStonesReceived);

                return response;
        }

        @Override
        @Transactional
        public WalletBalanceResponse exchangeSpiritStoneByAmount(ExchangeSpiritStoneByAmountRequest request,
                        Long userId) {
                log.info("Thực hiện đổi linh thạch theo số tiền cho user ID: {}, amount: {}", userId,
                                request.getAmount());

                // Kiểm tra số dư tiền mặt
                UserWallet userWallet = userWalletRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví của người dùng"));

                // Đổi theo số tiền
                int amountToSpend = request.getAmount();
                int spiritStonesReceived = (int) (amountToSpend / EXCHANGE_RATE);
                String description = "Đổi linh thạch - Số tiền: " + amountToSpend + " VND, Nhận: "
                                + spiritStonesReceived
                                + " linh thạch";

                // Kiểm tra số dư đủ không
                if (userWallet.getBalance() < amountToSpend) {
                        throw new InsufficientBalanceException("Số dư tiền mặt không đủ để đổi linh thạch. Cần: "
                                        + amountToSpend + " VND, Hiện có: " + userWallet.getBalance() + " VND");
                }

                // Thực hiện giao dịch
                performExchange(userWallet, userId, amountToSpend, spiritStonesReceived, description);

                // Tạo response
                WalletBalanceResponse response = new WalletBalanceResponse();
                response.setBalance(userWallet.getBalance());
                response.setSpiritStones(userWallet.getSpiritStones());
                response.setAmountSpent(amountToSpend);
                response.setSpiritStonesReceived(spiritStonesReceived);
                response.setDescription(description);

                log.info("Đổi linh thạch theo số tiền thành công. User ID: {}, Chi: {} VND, Nhận: {} linh thạch",
                                userId, amountToSpend, spiritStonesReceived);

                return response;
        }

        /**
         * Thực hiện giao dịch đổi linh thạch
         */
        private void performExchange(UserWallet userWallet, Long userId, int amountToSpend, int spiritStonesReceived,
                        String description) {
                // Trừ tiền mặt
                userWallet.setBalance(userWallet.getBalance() - amountToSpend);
                // Cộng linh thạch
                userWallet.setSpiritStones(userWallet.getSpiritStones() + spiritStonesReceived);
                userWalletRepository.save(userWallet);

                // Tạo User object để lưu transaction
                User user = new User();
                user.setId(userId);

                // Tạo 2 WalletTransaction
                // Giao dịch trừ tiền mặt
                WalletTransaction spendTransaction = new WalletTransaction();
                spendTransaction.setAmount(-amountToSpend);
                spendTransaction.setCurrency(WalletTransaction.CurrencyType.VND);
                spendTransaction.setType(TransactionType.SPIRIT_EXCHANGE);
                spendTransaction.setDescription(description);
                spendTransaction.setUser(user);
                walletTransactionRepository.save(spendTransaction);

                // Giao dịch cộng linh thạch
                WalletTransaction earnTransaction = new WalletTransaction();
                earnTransaction.setAmount(spiritStonesReceived);
                earnTransaction.setCurrency(WalletTransaction.CurrencyType.SPIRIT_STONE);
                earnTransaction.setType(TransactionType.SPIRIT_EARN);
                earnTransaction.setDescription("Nhận linh thạch từ đổi - " + description);
                earnTransaction.setUser(user);
                walletTransactionRepository.save(earnTransaction);
        }

        @Override
        public ExchangeHistoryListResponse getExchangeHistory(Long userId, Pageable pageable) {
                log.info("Lấy lịch sử đổi linh thạch cho user ID: {}", userId);

                // Tạo User object để query
                User user = new User();
                user.setId(userId);

                // Lấy các giao dịch SPIRIT_EXCHANGE (trừ tiền) và SPIRIT_EARN (cộng linh thạch)
                Page<WalletTransaction> transactions = walletTransactionRepository
                                .findByUserAndTypeInOrderByCreatedAtDesc(
                                                user,
                                                List.of(TransactionType.SPIRIT_EXCHANGE, TransactionType.SPIRIT_EARN),
                                                pageable);

                // Chuyển đổi thành response
                List<ExchangeHistoryResponse> exchanges = exchangeHistoryMapper
                                .toResponseList(transactions.getContent());

                ExchangeHistoryListResponse response = new ExchangeHistoryListResponse();
                response.setExchanges(exchanges);
                response.setTotalCount(transactions.getTotalElements());
                response.setTotalPages(transactions.getTotalPages());
                response.setCurrentPage(transactions.getNumber());
                response.setPageSize(transactions.getSize());

                return response;
        }
}
