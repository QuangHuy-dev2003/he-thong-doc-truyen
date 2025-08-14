-- Thêm các field cho voucher vào bảng sepay_topup_requests
ALTER TABLE sepay_topup_requests 
ADD COLUMN original_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
ADD COLUMN discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
ADD COLUMN voucher_code VARCHAR(50);

-- Cập nhật dữ liệu cũ (nếu có)
UPDATE sepay_topup_requests 
SET original_amount = amount 
WHERE original_amount = 0;

-- Tạo index cho voucher_code
CREATE INDEX idx_sepay_topup_requests_voucher_code ON sepay_topup_requests(voucher_code);
