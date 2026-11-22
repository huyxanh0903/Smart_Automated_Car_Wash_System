package controller;

import ai.rag.CarWashAiEngine;
import dao.CustomerDAO;
import dao.PromotionDAO;
import dao.ServiceDAO;
import dao.VehicleDAO;
import dto.Customer;
import dto.Promotion;
import dto.Service;
import dto.User;
import dto.Vehicle;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(name = "AiChatServlet", urlPatterns = {"/ai-chat", "/AiChatServlet"})
public class AiChatServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.print("{\"status\":\"ready\",\"message\":\"AI Car Wash Concierge is running.\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        String userMessage = "";

        // 1. Đọc tin nhắn từ FormData hoặc JSON body
        String paramMessage = request.getParameter("message");
        if (paramMessage != null && !paramMessage.trim().isEmpty()) {
            userMessage = paramMessage.trim();
        } else {
            // Đọc từ Request Payload JSON
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            String body = sb.toString();
            // Trích xuất "message": "..." đơn giản không cần Gson
            int idx = body.indexOf("\"message\"");
            if (idx != -1) {
                int colonIdx = body.indexOf(":", idx);
                int quoteStart = body.indexOf("\"", colonIdx + 1);
                int quoteEnd = body.indexOf("\"", quoteStart + 1);
                if (quoteStart != -1 && quoteEnd != -1) {
                    userMessage = body.substring(quoteStart + 1, quoteEnd);
                    userMessage = unescapeJson(userMessage);
                }
            }
        }

        if (userMessage.isEmpty()) {
            userMessage = "Xin chào! Gara có những gói dịch vụ rửa xe nào?";
        }

        // 2. Thu thập ngữ cảnh Người dùng (User Profile & Xe)
        StringBuilder userContext = new StringBuilder();
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("USER") != null) {
            User user = (User) session.getAttribute("USER");
            userContext.append("- Tên người dùng: ").append(user.getFullName()).append("\n");

            try {
                CustomerDAO customerDAO = new CustomerDAO();
                Customer customer = customerDAO.getCustomerByUserId(user.getUserId());
                if (customer != null) {
                    userContext.append("- Hạng thành viên: ").append(customer.getTierName() != null ? customer.getTierName() : "Standard")
                            .append(" (Ưu đãi giảm giá: ").append(customer.getDiscountPercent()).append("%)\n");
                    userContext.append("- Điểm tích lũy: ").append(customer.getTotalPoints()).append(" điểm\n");
                    userContext.append("- Số lần đã rửa tại gara: ").append(customer.getTotalWashes()).append(" lần\n");

                    // Xe đã đăng ký
                    VehicleDAO vehicleDAO = new VehicleDAO();
                    List<Vehicle> vehicles = vehicleDAO.getVehiclesByCustomerId(customer.getCustomerId());
                    if (vehicles != null && !vehicles.isEmpty()) {
                        userContext.append("- Phương tiện sở hữu:\n");
                        for (Vehicle v : vehicles) {
                            userContext.append("  + ")
                                    .append(v.getBrandDisplay() != null ? v.getBrandDisplay() : "")
                                    .append(" ")
                                    .append(v.getModelDisplay() != null ? v.getModelDisplay() : "")
                                    .append(" (Loại: ").append(v.getVehicleType() != null ? v.getVehicleType() : "Sedan")
                                    .append(", Biển số: ").append(v.getPlateNumber()).append(")\n");
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[AiChatServlet] Error loading user context: " + e.getMessage());
            }
        } else {
            userContext.append("Khách vãng lai chưa đăng nhập hệ thống.\n");
        }

        // 3. Thu thập ngữ cảnh Dịch vụ thực tế từ Database
        StringBuilder servicesContext = new StringBuilder();
        try {
            ServiceDAO serviceDAO = new ServiceDAO();
            List<Service> services = serviceDAO.getActiveServices();
            if (services != null) {
                for (Service s : services) {
                    servicesContext.append("• ").append(s.getServiceName())
                            .append(" (Giá: ").append(String.format("%,d", s.getPrice())).append(" VNĐ")
                            .append(", Thời lượng: ").append(s.getDurationMinutes()).append(" phút)")
                            .append(": ").append(s.getDescription() != null ? s.getDescription() : "")
                            .append("\n");
                }
            }
        } catch (Exception e) {
            System.err.println("[AiChatServlet] Error loading services context: " + e.getMessage());
        }

        // 4. Thu thập ngữ cảnh Khuyến mãi thực tế từ Database
        StringBuilder promosContext = new StringBuilder();
        try {
            PromotionDAO promoDAO = new PromotionDAO();
            List<Promotion> promos = promoDAO.getAllActivePromotions();
            if (promos != null && !promos.isEmpty()) {
                for (Promotion p : promos) {
                    promosContext.append("• Mã [").append(p.getCode()).append("]: ")
                            .append(p.getTitle() != null ? p.getTitle() : "Khuyến mãi")
                            .append(" - Giảm ")
                            .append("PERCENT".equalsIgnoreCase(p.getDiscountType()) ? p.getDiscountValue() + "%" : String.format("%,d", p.getDiscountValue()) + " VNĐ")
                            .append(" (Đến ngày: ").append(p.getEndDate()).append(")\n");
                }
            }
        } catch (Exception e) {
            System.err.println("[AiChatServlet] Error loading promotions context: " + e.getMessage());
        }

        // 5. Gọi RAG AI Engine
        CarWashAiEngine engine = CarWashAiEngine.getInstance();
        String aiReply = engine.askAi(
                userMessage,
                userContext.toString(),
                servicesContext.toString(),
                promosContext.toString()
        );

        // 6. Trả về JSON cho Frontend Widget
        try (PrintWriter out = response.getWriter()) {
            out.print("{\"status\":\"success\",\"reply\":" + escapeJson(aiReply) + "}");
        }
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
}
