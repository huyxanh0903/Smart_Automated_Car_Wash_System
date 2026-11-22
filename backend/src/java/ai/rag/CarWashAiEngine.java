package ai.rag;

import java.util.List;

public class CarWashAiEngine {

    private static CarWashAiEngine instance;
    private final KnowledgeBaseService knowledgeBaseService;
    private final GeminiService geminiService;

    private CarWashAiEngine() {
        this.knowledgeBaseService = KnowledgeBaseService.getInstance();
        this.geminiService = new GeminiService();
    }

    public static synchronized CarWashAiEngine getInstance() {
        if (instance == null) {
            instance = new CarWashAiEngine();
        }
        return instance;
    }

    /**
     * Xử lý câu hỏi người dùng với RAG (Retrieval-Augmented Generation)
     * kết hợp tri thức cẩm nang + ngữ cảnh người dùng & dịch vụ thực tế từ database.
     *
     * @param userMessage Câu hỏi hoặc yêu cầu của người dùng
     * @param userProfileContext Thông tin khách hàng (tên, hạng thành viên, xe đang sở hữu)
     * @param dbServicesContext Danh sách các gói dịch vụ và bảng giá thực tế từ DB
     * @param dbPromotionsContext Các chương trình khuyến mãi/voucher đang áp dụng từ DB
     * @return Câu trả lời được cá nhân hóa
     */
    public String askAi(String userMessage, String userProfileContext, String dbServicesContext, String dbPromotionsContext) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "Chào bạn! Tôi là trợ lý ảo chăm sóc xe thông minh. Bạn cần tôi hỗ trợ gì về dịch vụ rửa xe hoặc đặt lịch hôm nay?";
        }

        // 1. Retrieval: Tìm kiếm các đoạn tài liệu RAG liên quan nhất
        List<KnowledgeChunk> relevantChunks = knowledgeBaseService.retrieveRelevantChunks(userMessage, 3);
        
        StringBuilder ragKnowledgeText = new StringBuilder();
        if (relevantChunks != null && !relevantChunks.isEmpty()) {
            for (KnowledgeChunk chunk : relevantChunks) {
                ragKnowledgeText.append("--- [TÀI LIỆU: ").append(chunk.getTitle()).append("] ---\n");
                ragKnowledgeText.append(chunk.getContent()).append("\n\n");
            }
        } else {
            ragKnowledgeText.append("Áp dụng quy chuẩn dịch vụ rửa xe thông thường.\n");
        }

        // 2. Augmentation: Ghép toàn bộ ngữ cảnh vào System Prompt
        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("Bạn là 'Smart Wash Bot' - Trợ lý AI chuyên gia tư vấn chăm sóc xe và đặt lịch cho hệ sinh thái Rửa xe tự động thông minh (Smart Automated Car Wash System).\n");
        systemPrompt.append("Phong cách: Thân thiện, chu đáo, am hiểu kỹ thuật chăm sóc ô tô, ngắn gọn và rõ ràng. Trả lời bằng tiếng Việt lịch sự.\n\n");

        systemPrompt.append("=== NGUYÊN TẮC TƯ VẤN ===\n");
        systemPrompt.append("1. Khi khách hỏi về việc xe bị bẩn, ố mốc, trầy xước nhẹ, hãy dùng kiến thức bảo dưỡng xe để giải thích nguyên nhân và khuyến nghị gói dịch vụ phù hợp.\n");
        systemPrompt.append("2. Luôn ưu tiên gợi ý các dịch vụ có thực trong danh mục hệ thống dưới đây.\n");
        systemPrompt.append("3. Nếu có thông tin xe hoặc hạng thành viên của khách hàng, hãy cá nhân hóa câu trả lời (ví dụ: chúc mừng hạng Gold, nhắc nhở ưu đãi chiết khấu).\n");
        systemPrompt.append("4. Hướng dẫn khách hàng bấm vào nút 'Đặt lịch ngay' trên thanh menu hoặc chọn slot phù hợp để hoàn tất đặt chỗ.\n\n");

        if (userProfileContext != null && !userProfileContext.trim().isEmpty()) {
            systemPrompt.append("=== THÔNG TIN KHÁCH HÀNG HIỆN TẠI ===\n");
            systemPrompt.append(userProfileContext.trim()).append("\n\n");
        }

        if (dbServicesContext != null && !dbServicesContext.trim().isEmpty()) {
            systemPrompt.append("=== DANH MỤC DỊCH VỤ HIỆN CÓ CỦA HỆ THỐNG ===\n");
            systemPrompt.append(dbServicesContext.trim()).append("\n\n");
        }

        if (dbPromotionsContext != null && !dbPromotionsContext.trim().isEmpty()) {
            systemPrompt.append("=== KHUYẾN MÃI & VOUCHER ĐANG ÁP DỤNG ===\n");
            systemPrompt.append(dbPromotionsContext.trim()).append("\n\n");
        }

        systemPrompt.append("=== CƠ SỞ TRI THỨC CHUYÊN SÂU (RAG KNOWLEDGE RETRIEVED) ===\n");
        systemPrompt.append(ragKnowledgeText.toString());

        // 3. Generation: Gọi Gemini Service
        return geminiService.generateResponse(systemPrompt.toString(), userMessage);
    }
}
