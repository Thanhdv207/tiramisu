package com.bakery.database;

import com.bakery.shared.model.Product;
import com.bakery.shared.model.Order;
import com.bakery.shared.model.CartItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public class AdminDAO {

    // --- HÀM TRỢ GIÚP: TẠO CÂU LỆNH LỌC THỜI GIAN ---
    private String getCondition(String filter) {
        if ("Tuần này".equals(filter)) return "YEARWEEK(created_at, 1) = YEARWEEK(CURDATE(), 1)";
        if ("Tháng này".equals(filter)) return "YEAR(created_at) = YEAR(CURDATE()) AND MONTH(created_at) = MONTH(CURDATE())";
        if ("Năm nay".equals(filter)) return "YEAR(created_at) = YEAR(CURDATE())";
        return "DATE(created_at) = CURDATE()"; // Mặc định là "Hôm nay"
    }

    // --- 1. TỔNG DOANH THU THEO BỘ LỌC ---
    public double getRevenue(String filter) {
        String sql = "SELECT SUM(total_amount) AS total FROM orders WHERE " + getCondition(filter);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) return rs.getDouble("total");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // --- 2. TOP SẢN PHẨM THEO BỘ LỌC ---
    public Map<String, Integer> getTopSellingProducts(String filter) {
        Map<String, Integer> topProducts = new HashMap<>();
        String sql = "SELECT p.product_name, SUM(od.quantity) as total_qty FROM order_details od " +
                "JOIN products p ON od.product_id = p.product_id " +
                "JOIN orders o ON od.order_id = o.order_id " +
                "WHERE " + getCondition(filter) + " " +
                "GROUP BY od.product_id ORDER BY total_qty DESC LIMIT 5";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) topProducts.put(rs.getString("product_name"), rs.getInt("total_qty"));
        } catch (SQLException e) { e.printStackTrace(); }
        return topProducts;
    }

    // --- 3. DỮ LIỆU BIỂU ĐỒ CỘT THEO BỘ LỌC ---
    public Map<String, Double> getChartData(String filter) {
        Map<String, Double> map = new LinkedHashMap<>();

        if ("Năm nay".equals(filter)) {
            // Khởi tạo 12 tháng
            for (int i = 1; i <= 12; i++) map.put("T" + i, 0.0);
            String sql = "SELECT MONTH(created_at) as label, SUM(total_amount) as total FROM orders WHERE YEAR(created_at) = YEAR(CURDATE()) GROUP BY MONTH(created_at)";
            try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
                while(rs.next()) map.put("T" + rs.getInt("label"), rs.getDouble("total"));
            } catch (SQLException e) { e.printStackTrace(); }
        } else if ("Tháng này".equals(filter)) {
            // Lấy doanh thu từng ngày trong tháng hiện tại
            String sql = "SELECT DAY(created_at) as label, SUM(total_amount) as total FROM orders WHERE " + getCondition(filter) + " GROUP BY DAY(created_at)";
            try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
                while(rs.next()) map.put("Ngày " + rs.getInt("label"), rs.getDouble("total"));
            } catch (SQLException e) { e.printStackTrace(); }
        } else {
            // "Hôm nay" và "Tuần này" -> Hiển thị trend 7 ngày gần nhất cho dễ nhìn
            for (int i = 6; i >= 0; i--) {
                map.put(LocalDate.now().minusDays(i).format(DateTimeFormatter.ofPattern("dd/MM")), 0.0);
            }
            String sql = "SELECT DATE(created_at) as order_date, SUM(total_amount) as total FROM orders WHERE created_at >= CURDATE() - INTERVAL 6 DAY GROUP BY DATE(created_at)";
            try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
                while(rs.next()) {
                    java.sql.Date sqlDate = rs.getDate("order_date");
                    if (sqlDate != null) map.put(sqlDate.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM")), rs.getDouble("total"));
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }
        return map;
    }

    // --- 4. LẤY LỊCH SỬ ĐƠN HÀNG CÓ KÈM BỘ LỌC THỜI GIAN ---
// --- 4. LẤY LỊCH SỬ ĐƠN HÀNG CÓ KÈM BỘ LỌC VÀ TÊN THU NGÂN ---
    public List<Order> getAllOrders(String filter) {
        List<Order> list = new ArrayList<>();
        // Đổi thành o.created_at để tránh bị nhầm cột khi JOIN bảng
        String condition = "Tất cả".equals(filter) ? "1=1" : getCondition(filter).replace("created_at", "o.created_at");

        // JOIN bảng users để lấy full_name của thu ngân
        String sql = "SELECT o.order_id, o.created_at, o.total_amount, o.payment_method, o.customer_name, o.customer_phone, u.full_name as cashier_name " +
                "FROM orders o " +
                "LEFT JOIN users u ON o.user_id = u.user_id " +
                "WHERE " + condition + " ORDER BY o.created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Order o = new Order(
                        rs.getString("order_id"),
                        rs.getString("created_at"),
                        rs.getString("customer_name"),
                        rs.getString("customer_phone"),
                        rs.getDouble("total_amount")
                );

                String method = rs.getString("payment_method");
                o.setPaymentMethod("CASH".equals(method) ? "Tiền mặt" : "Chuyển khoản");

                // Gán tên thu ngân (nếu lỡ đơn nào không có ID thì để "Không rõ")
                String cashier = rs.getString("cashier_name");
                o.setCashierName(cashier != null ? cashier : "Không rõ");

                list.add(o);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    // --- CÁC HÀM CŨ GIỮ NGUYÊN BÊN DƯỚI ---
    public List<CartItem> getOrderDetails(String orderId) {
        List<CartItem> list = new ArrayList<>();
        String sql = "SELECT od.product_id, p.product_name, od.quantity, od.unit_price FROM order_details od JOIN products p ON od.product_id = p.product_id WHERE od.order_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) list.add(new CartItem(rs.getString("product_id"), rs.getString("product_name"), rs.getInt("quantity"), rs.getDouble("unit_price")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Product> getAllProducts() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) list.add(new Product(rs.getString("product_id"), rs.getString("product_name"), rs.getInt("category_id"), rs.getDouble("price"), rs.getInt("stock_quantity")));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean addProduct(Product p) {
        String sql = "INSERT INTO products (product_id, product_name, category_id, price, stock_quantity) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, p.getProductId()); pstmt.setString(2, p.getProductName()); pstmt.setInt(3, p.getCategoryId()); pstmt.setDouble(4, p.getPrice()); pstmt.setInt(5, p.getStockQuantity());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean updateProduct(Product p) {
        String sql = "UPDATE products SET product_name=?, category_id=?, price=?, stock_quantity=? WHERE product_id=?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, p.getProductName()); pstmt.setInt(2, p.getCategoryId()); pstmt.setDouble(3, p.getPrice()); pstmt.setInt(4, p.getStockQuantity()); pstmt.setString(5, p.getProductId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean deleteProduct(String productId) {
        String sql = "DELETE FROM products WHERE product_id=?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, productId); return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // =========================================================================
    // --- HÀM MỚI BƠM VÀO: API TỔNG QUAN TOÀN BỘ CỬA HÀNG (GLOBAL ANALYTICS) ---
    // =========================================================================
    public String getGlobalAnalytics(String filter) {
        JsonObject root = new JsonObject();
        String timeCond = "1=1";

        if ("Hôm nay".equals(filter)) timeCond = "DATE(created_at) = CURDATE()";
        else if ("Tuần này".equals(filter)) timeCond = "YEARWEEK(created_at, 1) = YEARWEEK(CURDATE(), 1)";
        else if ("Tháng này".equals(filter)) timeCond = "YEAR(created_at) = YEAR(CURDATE()) AND MONTH(created_at) = MONTH(CURDATE())";
        else if ("Năm nay".equals(filter)) timeCond = "YEAR(created_at) = YEAR(CURDATE())";

        try (Connection conn = DatabaseConnection.getConnection()) {

            // 1. Doanh thu & Tổng đơn (Dùng IFNULL chuẩn MySQL)
            String sqlKpi = "SELECT COUNT(order_id) as TotalOrders, IFNULL(SUM(total_amount), 0) as TotalRevenue FROM orders WHERE " + timeCond;
            try (PreparedStatement pst = conn.prepareStatement(sqlKpi); ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    root.addProperty("globalOrders", rs.getInt("TotalOrders"));
                    root.addProperty("globalRevenue", rs.getDouble("TotalRevenue"));
                }
            }

            // 2. Top 5 Nhân viên
            String sqlTopEmp = "SELECT u.full_name, COUNT(o.order_id) as OrderCount, SUM(o.total_amount) as Revenue " +
                    "FROM orders o JOIN users u ON o.user_id = u.user_id " +
                    "WHERE " + timeCond.replace("created_at", "o.created_at") +
                    " GROUP BY u.user_id, u.full_name ORDER BY Revenue DESC LIMIT 5";
            JsonArray topEmpArr = new JsonArray();
            try (PreparedStatement pst = conn.prepareStatement(sqlTopEmp); ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    JsonObject emp = new JsonObject();
                    emp.addProperty("name", rs.getString("full_name"));
                    emp.addProperty("orders", rs.getInt("OrderCount"));
                    emp.addProperty("revenue", rs.getDouble("Revenue"));
                    topEmpArr.add(emp);
                }
            }
            root.add("topEmployees", topEmpArr);

            // 3. Top 5 Sản phẩm
            String sqlTopProd = "SELECT p.product_name, SUM(od.quantity) as TotalSold, SUM(od.quantity * od.unit_price) as ProdRev " +
                    "FROM order_details od JOIN products p ON od.product_id = p.product_id " +
                    "JOIN orders o ON od.order_id = o.order_id WHERE " + timeCond.replace("created_at", "o.created_at") +
                    " GROUP BY p.product_id, p.product_name ORDER BY TotalSold DESC LIMIT 5";
            JsonArray topProdArr = new JsonArray();
            try (PreparedStatement pst = conn.prepareStatement(sqlTopProd); ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    JsonObject prod = new JsonObject();
                    prod.addProperty("name", rs.getString("product_name"));
                    prod.addProperty("qty", rs.getInt("TotalSold"));
                    prod.addProperty("revenue", rs.getDouble("ProdRev"));
                    topProdArr.add(prod);
                }
            }
            root.add("globalTopProducts", topProdArr);

            root.addProperty("status", "SUCCESS");
            return root.toString();

        } catch (Exception e) {
            e.printStackTrace();
            JsonObject err = new JsonObject();
            err.addProperty("status", "ERROR");
            return err.toString();
        }
    }
}