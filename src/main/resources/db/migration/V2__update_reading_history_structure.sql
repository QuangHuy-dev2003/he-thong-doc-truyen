-- Migration để cập nhật cấu trúc bảng reading_history
-- Từ composite key (user_id, chapter_id) thành (user_id, story_id)

-- Bước 1: Tạo bảng tạm để lưu dữ liệu mới
CREATE TABLE reading_history_new (
    user_id BIGINT NOT NULL,
    story_id BIGINT NOT NULL,
    chapter_id BIGINT NOT NULL,
    last_read_at TIMESTAMP NOT NULL,
    PRIMARY KEY (user_id, story_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (story_id) REFERENCES stories(id) ON DELETE CASCADE,
    FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE CASCADE
);

-- Bước 2: Chuyển đổi dữ liệu từ bảng cũ sang bảng mới
-- Mỗi story chỉ giữ lại chapter có chapter_id lớn nhất (mới nhất)
INSERT INTO reading_history_new (user_id, story_id, chapter_id, last_read_at)
SELECT 
    rh.user_id,
    c.story_id,
    rh.chapter_id,
    rh.last_read_at
FROM reading_history rh
INNER JOIN chapters c ON rh.chapter_id = c.id
WHERE (rh.user_id, c.story_id, rh.chapter_id) IN (
    SELECT 
        rh2.user_id,
        c2.story_id,
        MAX(rh2.chapter_id) as max_chapter_id
    FROM reading_history rh2
    INNER JOIN chapters c2 ON rh2.chapter_id = c2.id
    GROUP BY rh2.user_id, c2.story_id
);

-- Bước 3: Xóa bảng cũ
DROP TABLE reading_history;

-- Bước 4: Đổi tên bảng mới thành tên cũ
ALTER TABLE reading_history_new RENAME TO reading_history;

-- Bước 5: Tạo index để tối ưu hiệu năng
CREATE INDEX idx_reading_history_user_id ON reading_history(user_id);
CREATE INDEX idx_reading_history_story_id ON reading_history(story_id);
CREATE INDEX idx_reading_history_last_read_at ON reading_history(last_read_at);
CREATE INDEX idx_reading_history_user_story ON reading_history(user_id, story_id);
