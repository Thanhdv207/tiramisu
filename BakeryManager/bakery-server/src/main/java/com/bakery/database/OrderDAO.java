package com.bakery.database;

import com.bakery.shared.model.CartItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OrderDAO {

    // --- HÀM TẠO MÃ ĐƠN HÀNG TỰ ĐỘNG ---
    private String generateOrderId() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        return "ORD-" + dtf.format(LocalDateTime.now());
    }

    // --- HÀM LƯU ĐƠN HÀNG VÀO DATABASE ---
    public boolean createOrder(List<CartItem> cartList, double totalAmount, int userId, String customerName, String customerPhone, String paymentMethod) {
        String orderId = generateOrderId();
        Connection conn = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Bắt đầu Transaction (Đảm bảo an toàn dữ liệu)

            // 1. Lưu thông tin chung của hóa đơn vào bảng 'orders'
            String sqlOrder = "INSERT INTO orders (order_id, user_id, total_amount, payment_method, customer_name, customer_phone) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlOrder)) {
                pstmt.setString(1, orderId);
                pstmt.setInt(2, userId);
                pstmt.setDouble(3, totalAmount);
                pstmt.setString(4, paymentMethod); // CASH hoặc CK
                pstmt.setString(5, customerName);
                pstmt.setString(6, customerPhone);
                pstmt.executeUpdate();
            }

            // 2. Lưu chi tiết từng món bánh và trừ số lượng trong kho
            String sqlDetail = "INSERT INTO order_details (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
            String sqlUpdateStock = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ?";

            try (PreparedStatement pstmtDetail = conn.prepareStatement(sqlDetail);
                 PreparedStatement pstmtStock = conn.prepareStatement(sqlUpdateStock)) {

                for (CartItem item : cartList) {
                    // Thêm vào bảng order_details
                    pstmtDetail.setString(1, orderId);
                    pstmtDetail.setString(2, item.getProductId());
                    pstmtDetail.setInt(3, item.getQuantity());
                    pstmtDetail.setDouble(4, item.getUnitPrice());
                    pstmtDetail.addBatch();

                    // Trừ kho trong bảng products
                    pstmtStock.setInt(1, item.getQuantity());
                    pstmtStock.setString(2, item.getProductId());
                    pstmtStock.addBatch();
                }

                // Thực thi cùng lúc toàn bộ danh sách bánh
                pstmtDetail.executeBatch();
                pstmtStock.executeBatch();
            }

            conn.commit(); // Xác nhận lưu thành công toàn bộ
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if (conn != null) conn.rollback(); // Lỗi thì hoàn tác, không trừ kho lung tung
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close(); // Đóng kết nối
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}