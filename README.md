# Tiệm Truyện Mèo Béo - Dự án Web Đọc Truyện Cá Nhân

> "Nơi tụ hội các đạo hữu, đọc truyện tiên hiệp, kiếm hiệp, ngôn tình và hơn thế nữa!"

## Giới thiệu

Đây là một **dự án cá nhân** xây dựng hệ thống web đọc truyện online, hỗ trợ người dùng với tính năng nổi bật:

- Quản lý truyện, chương, thể loại, tác giả.
- Người dùng có thể đăng ký, đăng nhập (hỗ trợ Google OAuth2).
- Hệ thống VIP, chương VIP, nạp tiền qua VNPAY/Sepay.
- Điểm danh nhận linh thạch mỗi ngày.
- Nhận và gửi email OTP (xác minh, quên mật khẩu, v.v).
- Chia sẻ doanh thu giữa tác giả và admin.
- Lịch sử đọc, đánh dấu yêu thích, đánh giá truyện.
- Bảo mật với Spring Security + JWT.
- Lưu trữ ảnh qua Cloudinary, hỗ trợ WebP.
- Tách biệt rõ ràng backend (Java Spring Boot) và frontend (ReactJS/Vite).

## ⚙️ Công nghệ sử dụng

| Layer    | Stack                                  |
| -------- | -------------------------------------- |
| Backend  | Java 17, Spring Boot, Spring Security  |
| Database | PostgreSQL (hosted by Neon.tech)       |
| Frontend | ReactJS + Tailwind + Axios             |
| Auth     | OAuth2 (Google), JWT, RefreshToken     |
| Storage  | Cloudinary                             |
| CI/CD    | Vercel (frontend), Render (backend)    |
| Others   | Lombok, JPA, Gradle, MapStruct, dotenv |

## Tính năng chính

- [x] Đăng ký, đăng nhập (hỗ trợ Google OAuth2)
- [x] Xem danh sách truyện, tìm kiếm, phân trang
- [x] Đọc truyện, đánh dấu chương đã đọc
- [x] Mua chương VIP bằng ví Linh Thạch
- [x] Điểm danh nhận Linh Thạch mỗi ngày
- [x] Quản lý truyện cá nhân (cho tác giả)
- [x] Quản trị viên phê duyệt truyện, xử lý báo cáo
- [x] Hệ thống gửi OTP, thông báo, badge
- [x] Chia sẻ doanh thu: 80% tác giả - 20% admin

## 🛠️ Hướng dẫn cài đặt

### Yêu cầu:

- Java 17
- PostgreSQL (hoặc Neon.tech)
- Node.js (nếu chạy frontend)
- Gradle

### Clone và cấu hình

git clone https://github.com/your-username/tiem-truyen-meo-beo.git

### Tạo file .env

DB_URL=jdbc:postgresql://<host>:<port>/<db_name>
DB_USERNAME=...
DB_PASSWORD=...
JWT_SECRET=...
CLOUD_NAME=...
API_KEY=...
API_SECRET=...
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
SPRING_MAIL_USERNAME=...
SPRING_MAIL_PASSWORD=...
VNPAY_TMN_CODE=...
VNPAY_HASH_SECRET=...

## Giấy phép sử dụng

Dự án này được phát triển cá nhân nhằm mục đích học tập, phi lợi nhuận.

Mọi góp ý, pull request đều được hoan nghênh!

## Cấu trúc dự án

```bash
src
├── config             # Cấu hình chung: CORS, Swagger, Mail, Security
├── controller         # API endpoint các chức năng
├── domain             # Entity + DTO
│   ├── entity
│   └── dto
├── repository         # JPA repository + custom repo
├── service
│   ├── impl           # Logic xử lý chính
│   └── interface      # Định nghĩa hàm
├── security           # JWT filter, provider, handler, config
├── utils              # Tiện ích dùng chung: format, slug, token...
├── mapper             # MapStruct convert giữa Entity ↔ DTO
└── ...

```
