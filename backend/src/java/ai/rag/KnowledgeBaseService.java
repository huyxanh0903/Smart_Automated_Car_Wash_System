package ai.rag;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

public class KnowledgeBaseService {

    private static KnowledgeBaseService instance;
    private final List<KnowledgeChunk> chunks = new ArrayList<>();

    private KnowledgeBaseService() {
        loadKnowledgeBase();
    }

    public static synchronized KnowledgeBaseService getInstance() {
        if (instance == null) {
            instance = new KnowledgeBaseService();
        }
        return instance;
    }

    private void loadKnowledgeBase() {
        loadDocument("ai/knowledge/car_care_knowledge.txt", "Chăm sóc xe");
        loadDocument("ai/knowledge/service_policy_guide.txt", "Chính sách & Quy định");
    }

    private void loadDocument(String resourcePath, String category) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("[KnowledgeBaseService] Warning: Could not find resource " + resourcePath);
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder currentChunk = new StringBuilder();
                String currentTitle = "Tổng quan";
                String line;
                int chunkIndex = 1;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) {
                        continue;
                    }

                    // Nhận diện tiêu đề mục lớn (1. , 2. , 3. hoặc [TIÊU ĐỀ])
                    if (line.matches("^[0-9]+\\..*") || (line.startsWith("[") && line.endsWith("]"))) {
                        if (currentChunk.length() > 0) {
                            chunks.add(new KnowledgeChunk(
                                    category + "_" + chunkIndex++,
                                    currentTitle,
                                    category,
                                    currentChunk.toString().trim()
                            ));
                            currentChunk.setLength(0);
                        }
                        currentTitle = line.replaceAll("[\\[\\]]", "").trim();
                    } else {
                        currentChunk.append(line).append("\n");
                    }
                }

                if (currentChunk.length() > 0) {
                    chunks.add(new KnowledgeChunk(
                        category + "_" + chunkIndex++,
                        currentTitle,
                        category,
                        currentChunk.toString().trim()
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("[KnowledgeBaseService] Error loading " + resourcePath + ": " + e.getMessage());
        }
    }

    /**
     * Tìm kiếm top K chunks có liên quan nhất với truy vấn của người dùng.
     * Sử dụng chấm điểm từ khóa kết hợp chuẩn hóa tiếng Việt không dấu.
     */
    public List<KnowledgeChunk> retrieveRelevantChunks(String query, int topK) {
        if (query == null || query.trim().isEmpty() || chunks.isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedQuery = removeAccents(query.toLowerCase());
        Set<String> queryWords = extractKeywords(normalizedQuery);

        List<ScoredChunk> scoredList = new ArrayList<>();

        for (KnowledgeChunk chunk : chunks) {
            String normalizedTitle = removeAccents(chunk.getTitle().toLowerCase());
            String normalizedContent = removeAccents(chunk.getContent().toLowerCase());

            double score = 0;

            for (String word : queryWords) {
                if (word.length() < 2) continue; // Bỏ qua từ quá ngắn

                // Trùng khớp trong Tiêu đề được cộng điểm cao
                if (normalizedTitle.contains(word)) {
                    score += 5.0;
                }

                // Trùng khớp trong Nội dung
                if (normalizedContent.contains(word)) {
                    score += 1.5;
                }
            }

            if (score > 0) {
                scoredList.add(new ScoredChunk(chunk, score));
            }
        }

        // Sắp xếp giảm dần theo điểm liên quan
        scoredList.sort((a, b) -> Double.compare(b.score, a.score));

        List<KnowledgeChunk> results = new ArrayList<>();
        int count = Math.min(topK, scoredList.size());
        for (int i = 0; i < count; i++) {
            results.add(scoredList.get(i).chunk);
        }

        // Nếu không khớp từ khóa nào nhưng có câu hỏi, trả về tối đa 2 chunks tổng quan
        if (results.isEmpty() && !chunks.isEmpty()) {
            results.add(chunks.get(0));
            if (chunks.size() > 1) {
                results.add(chunks.get(1));
            }
        }

        return results;
    }

    private Set<String> extractKeywords(String text) {
        Set<String> set = new HashSet<>();
        String[] tokens = text.split("[\\s,?.!;:\"]+");
        for (String t : tokens) {
            if (!t.isEmpty()) {
                set.add(t);
            }
        }
        return set;
    }

    public static String removeAccents(String s) {
        if (s == null) return "";
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }

    public int getTotalChunks() {
        return chunks.size();
    }

    private static class ScoredChunk {
        KnowledgeChunk chunk;
        double score;

        ScoredChunk(KnowledgeChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }
    }
}
