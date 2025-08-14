-- Cập nhật format transfer_content từ NAP_x_y thành NAPxy (loại bỏ dấu gạch dưới)
UPDATE sepay_topup_requests 
SET transfer_content = REPLACE(transfer_content, '_', '')
WHERE transfer_content LIKE 'NAP_%_%';

-- Log số lượng record đã cập nhật
SELECT COUNT(*) as updated_records FROM sepay_topup_requests WHERE transfer_content LIKE 'NAP%' AND transfer_content NOT LIKE 'NAP_%';
