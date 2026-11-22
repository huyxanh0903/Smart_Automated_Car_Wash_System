package ai.config;

public class AiConfig {
    
    // Khuyến nghị đặt biến môi trường GEMINI_API_KEY trên máy hoặc server
    // Hoặc có thể cấu hình trực tiếp vào biến DEFAULT_API_KEY bên dưới
    public static final String DEFAULT_API_KEY = ""; 
    
    // Model Gemini khuyến nghị: gemini-1.5-flash hoặc gemini-2.0-flash
    public static final String MODEL_NAME = "gemini-1.5-flash";
    
    public static final String GEMINI_API_ENDPOINT = 
            "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_NAME + ":generateContent";
            
    public static String getApiKey() {
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) {
            return envKey.trim();
        }
        String propKey = System.getProperty("gemini.api.key");
        if (propKey != null && !propKey.trim().isEmpty()) {
            return propKey.trim();
        }
        return DEFAULT_API_KEY;
    }
    
    public static boolean hasApiKey() {
        String key = getApiKey();
        return key != null && !key.trim().isEmpty();
    }
}
