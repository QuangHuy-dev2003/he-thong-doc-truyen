package com.meobeo.truyen.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
@Slf4j
public class ContentFilterUtil {

    // Danh sách từ cấm (có thể mở rộng hoặc load từ database)
    private static final List<String> BANNED_WORDS = Arrays.asList(
            "fuck", "shit", "damn", "bitch", "asshole", "bastard",
            "đm", "dm", "đmm", "dmm", "vcl", "vãi", "chết", "cứt",
            "đĩ", "cave", "lồn", "buồi", "cặc", "địt", "cl"
    // Có thể thêm nhiều từ cấm khác
    );

    // Pattern để check từ cấm (không phân biệt chữ hoa/thường, có thể có số hoặc ký
    // tự đặc biệt xen kẽ)
    private static final List<Pattern> BANNED_PATTERNS = BANNED_WORDS.stream()
            .map(word -> Pattern.compile(
                    "(?i).*" + word.chars()
                            .mapToObj(c -> "[" + (char) c + "]")
                            .reduce("", (a, b) -> a + "[\\d\\W]*" + b) + ".*"))
            .toList();

    /**
     * Kiểm tra xem nội dung có chứa từ ngữ không phù hợp không
     */
    public boolean containsInappropriateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        String normalizedContent = normalizeText(content);

        // Check exact banned words
        for (String bannedWord : BANNED_WORDS) {
            if (normalizedContent.toLowerCase().contains(bannedWord.toLowerCase())) {
                log.warn("Detected banned word: {} in content", bannedWord);
                return true;
            }
        }

        // Check patterns với ký tự xen kẽ
        for (Pattern pattern : BANNED_PATTERNS) {
            if (pattern.matcher(normalizedContent).matches()) {
                log.warn("Detected banned pattern in content: {}", content);
                return true;
            }
        }

        return false;
    }

    /**
     * Làm sạch text để dễ kiểm tra (remove các ký tự đặc biệt, số)
     */
    private String normalizeText(String text) {
        return text.replaceAll("[\\d\\p{Punct}\\s]+", "").toLowerCase();
    }

    /**
     * Lấy message lỗi khi content không phù hợp
     */
    public String getInappropriateContentMessage() {
        return "Nội dung comment chứa từ ngữ không phù hợp. Vui lòng sử dụng ngôn từ lịch sự.";
    }

    /**
     * Validate content và throw exception nếu không phù hợp
     */
    public void validateContent(String content) {
        if (containsInappropriateContent(content)) {
            throw new IllegalArgumentException(getInappropriateContentMessage());
        }
    }
}
