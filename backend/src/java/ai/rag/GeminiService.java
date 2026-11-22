package ai.rag;

import ai.config.AiConfig;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeminiService {

    public GeminiService() {
    }

    /**
     * Gọi Gemini API để sinh câu trả lời với System Prompt và User Message.
     */
    public String generateResponse(String systemPrompt, String userMessage) {
        String apiKey = AiConfig.getApiKey();

        if (apiKey == null || apiKey.trim().isEmpty()) {
            return generateLocalFallback(userMessage, systemPrompt);
        }

        try {
            String endpoint = AiConfig.GEMINI_API_ENDPOINT + "?key=" + apiKey;

            // Xây dựng JSON request payload chuẩn Gemini API
            String combinedPrompt = (systemPrompt != null && !systemPrompt.isEmpty())
                    ? systemPrompt + "\n\n[CÂU HỎI CỦA KHÁCH HÀNG]: " + userMessage
                    : userMessage;

            String jsonPayload = "{"
                    + "\"contents\": [{"
                    + "  \"role\": \"user\","
                    + "  \"parts\": [{\"text\": " + escapeJson(combinedPrompt) + "}]"
                    + "}],"
                    + "\"generationConfig\": {"
                    + "  \"temperature\": 0.4,"
                    + "  \"maxOutputTokens\": 1000"
                    + "}"
                    + "}";

            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);

            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(input, 0, input.length);
            }

            int statusCode = conn.getResponseCode();
            InputStream is = (statusCode >= 200 && statusCode < 300) 
                    ? conn.getInputStream() 
                    : conn.getErrorStream();

            StringBuilder response = new StringBuilder();
            if (is != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }
            }

            if (statusCode == 200) {
                return extractTextFromGeminiResponse(response.toString());
            } else {
                System.err.println("[GeminiService] HTTP " + statusCode + ": " + response);
                return "Hệ thống AI đang bảo trì tạm thời (Mã lỗi " + statusCode + "). Dưới đây là thông tin tham khảo:\n"
                        + generateLocalFallback(userMessage, systemPrompt);
            }
        } catch (Exception e) {
            System.err.println("[GeminiService] Error calling API: " + e.getMessage());
            return generateLocalFallback(userMessage, systemPrompt);
        }
    }

    /**
     * Trích xuất văn bản từ phản hồi JSON của Gemini mà không phụ thuộc vào thư viện ngoài.
     */
    private String extractTextFromGeminiResponse(String json) {
        try {
            // Định dạng thông thường: "text": "..."
            Pattern pattern = Pattern.compile("\"text\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"");
            Matcher matcher = pattern.matcher(json);
            if (matcher.find()) {
                String text = matcher.group(1);
                return unescapeJson(text);
            }
        } catch (Exception e) {
            System.err.println("[GeminiService] Error parsing JSON text: " + e.getMessage());
        }
        return "Xin lỗi, hiện tại tôi chưa xử lý được câu trả lời này.";
    }

    private String escapeJson(String raw) {
        if (raw == null) return "\"\"";
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        for (char c : raw.toCharArray()) {
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    private String unescapeJson(String escaped) {
        if (escaped == null) return "";
        return escaped
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    /**
     * Phản hồi dự phòng thông minh khi chưa có API Key hoặc bị gián đoạn mạng
     */
    private String generateLocalFallback(String userMessage, String systemPrompt) {
        return "🤖 [Hệ thống Trợ lý ảo Car Wash]:\n\n"
                + "Chào bạn! Tôi có thể tư vấn chăm sóc xe, hướng dẫn đặt lịch và giải đáp chính sách dịch vụ.\n"
                + "💡 Để kích hoạt sức mạnh trí tuệ nhân tạo Gemini đầy đủ, vui lòng cấu hình biến môi trường GEMINI_API_KEY.\n\n"
                + "Một số câu hỏi bạn có thể thử:\n"
                + "• Xe SUV bám bùn đất sau mưa nên chọn gói rửa nào?\n"
                + "• Quy định hủy lịch và hoàn tiền của gara như thế nào?\n"
                + "• Hạng thành viên Gold được ưu đãi những gì?";
    }
}
