# 🔧 Hướng dẫn Setup File .env

## 📋 Tạo file .env

Tạo file `.env` trong thư mục gốc của project với nội dung sau:

```env
# ========================================
# CONFIGURATION FOR TIEM TRUYEN MEO BEO
# ========================================

# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=tiem_truyen_meo_beo
DB_USERNAME=root
DB_PASSWORD=your_password_here

# JWT Configuration
JWT_SECRET=your_jwt_secret_key_here_make_it_long_and_secure
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Server Configuration
SERVER_PORT=8080
SERVER_HOST=localhost

# Email Configuration (nếu sử dụng email)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your_email@gmail.com
SMTP_PASSWORD=your_app_password

# Redis Configuration (nếu sử dụng Redis cache)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0

# File Upload Configuration
UPLOAD_PATH=./uploads
MAX_FILE_SIZE=10485760

# Payment Configuration (nếu tích hợp thanh toán)
PAYPAL_CLIENT_ID=your_paypal_client_id
PAYPAL_CLIENT_SECRET=your_paypal_client_secret
PAYPAL_MODE=sandbox

# External API Keys (nếu sử dụng)
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# Logging Configuration
LOG_LEVEL=INFO
LOG_FILE_PATH=./logs/application.log

# Development/Production Mode
SPRING_PROFILES_ACTIVE=dev
DEBUG_MODE=true

# ========================================
# CUSTOM CONFIGURATIONS
# ========================================

# VIP Configuration
VIP_DISCOUNT_PERCENT=20
VIP_DAILY_BONUS=100

# Story Configuration
MAX_CHAPTERS_PER_STORY=1000
MAX_CONTENT_LENGTH=50000

# User Configuration
MAX_FAVORITES_PER_USER=100
MAX_COMMENTS_PER_DAY=50

# ========================================
# SECURITY CONFIGURATIONS
# ========================================

# Password Policy
MIN_PASSWORD_LENGTH=8
REQUIRE_SPECIAL_CHAR=true
PASSWORD_EXPIRY_DAYS=90

# Rate Limiting
MAX_LOGIN_ATTEMPTS=5
LOGIN_LOCKOUT_DURATION=300000

# ========================================
# FEATURE FLAGS
# ========================================

ENABLE_EMAIL_VERIFICATION=true
ENABLE_SMS_VERIFICATION=false
ENABLE_SOCIAL_LOGIN=true
ENABLE_VIP_FEATURES=true
ENABLE_PAYMENT=true
ENABLE_COMMENTS=true
ENABLE_RATINGS=true
```

## 🚀 Cách sử dụng trong code

### 1. Sử dụng EnvUtils (Khuyến nghị)

```java
@Service
public class UserService {

    private final EnvUtils envUtils;

    public UserService(EnvUtils envUtils) {
        this.envUtils = envUtils;
    }

    public void someMethod() {
        // Lấy giá trị string
        String dbHost = envUtils.get("DB_HOST", "localhost");

        // Lấy giá trị integer với default
        Integer maxFavorites = envUtils.getInt("MAX_FAVORITES_PER_USER", 100);

        // Lấy giá trị boolean
        Boolean enableVip = envUtils.getBoolean("ENABLE_VIP_FEATURES", true);

        // Kiểm tra biến có tồn tại không
        if (envUtils.has("PAYPAL_CLIENT_ID")) {
            // Xử lý logic
        }
    }
}
```

### 2. Sử dụng trực tiếp Dotenv

```java
@Component
public class DatabaseConfig {

    private final Dotenv dotenv;

    public DatabaseConfig(Dotenv dotenv) {
        this.dotenv = dotenv;
    }

    public String getDatabaseUrl() {
        String host = dotenv.get("DB_HOST", "localhost");
        String port = dotenv.get("DB_PORT", "3306");
        String name = dotenv.get("DB_NAME", "tiem_truyen_meo_beo");

        return String.format("jdbc:mysql://%s:%s/%s", host, port, name);
    }
}
```

## ⚠️ Lưu ý quan trọng

1. **Không commit file .env**: Thêm `.env` vào `.gitignore`
2. **Bảo mật**: Không chia sẻ file .env chứa thông tin nhạy cảm
3. **Validation**: Luôn kiểm tra giá trị trước khi sử dụng
4. **Default values**: Luôn có giá trị mặc định cho các biến quan trọng

## 🔍 Kiểm tra file .env đã load

Khi ứng dụng khởi động, bạn sẽ thấy log:

```
✅ File .env loaded successfully
📁 Environment variables loaded: 25
```

Nếu không thấy log này, kiểm tra:

- File `.env` có tồn tại trong thư mục gốc không
- Cú pháp file `.env` có đúng không
- Quyền đọc file có đủ không
