package ai.test;

import ai.rag.CarWashAiEngine;
import ai.rag.KnowledgeBaseService;
import ai.rag.KnowledgeChunk;
import java.util.List;

public class TestAiEngine {

    public static void main(String[] args) {
        System.out.println("========== BẮT ĐẦU KIỂM THỬ MODULE AI RAG ==========");

        // 1. Kiểm tra nạp Knowledge Base
        KnowledgeBaseService kb = KnowledgeBaseService.getInstance();
        System.out.println("[1] Tổng số chunks tri thức đã nạp: " + kb.getTotalChunks());

        // 2. Kiểm tra Retrieval (Tìm kiếm ngữ cảnh liên quan)
        String testQuery = "Xe SUV Mazda CX-5 bị dính nhiều bùn đất sau mưa và kính bị ố mốc thì rửa thế nào?";
        System.out.println("\n[2] Truy vấn thử nghiệm: " + testQuery);

        List<KnowledgeChunk> chunks = kb.retrieveRelevantChunks(testQuery, 2);
        System.out.println("-> Tìm thấy " + chunks.size() + " tài liệu liên quan:");
        for (KnowledgeChunk chunk : chunks) {
            System.out.println("   * [" + chunk.getCategory() + "] " + chunk.getTitle());
        }

        // 3. Kiểm tra CarWashAiEngine
        System.out.println("\n[3] Chạy thử CarWashAiEngine (RAG Pipeline)...");
        CarWashAiEngine engine = CarWashAiEngine.getInstance();

        String dummyUserContext = "- Khách hàng: Nguyễn Văn A\n- Hạng thành viên: Gold (Giảm giá 10%)\n- Phương tiện đã đăng ký: Mazda CX-5 (SUV) - Biển số: 51A-999.88";
        String dummyServices = "- Gói Rửa Tiêu Chuẩn: 70,000đ (20 phút)\n- Gói Rửa Bọt Tuyết Phủ Bóng: 120,000đ (30 phút)\n- Gói Chăm Sóc Toàn Diện + Tẩy Ố Kính: 250,000đ (45 phút)\n- Gói Vệ Sinh Khoang Máy: 350,000đ (60 phút)";
        String dummyPromotions = "- MÃ GOLDCARE: Giảm thêm 20,000đ cho hạng Gold khi đặt lịch trước";

        String aiResponse = engine.askAi(testQuery, dummyUserContext, dummyServices, dummyPromotions);

        System.out.println("\n[4] KẾT QUẢ PHẢN HỒI:");
        System.out.println("--------------------------------------------------");
        System.out.println(aiResponse);
        System.out.println("--------------------------------------------------");

        System.out.println("\n========== HOÀN THÀNH KIỂM THỬ ==========");
    }
}
