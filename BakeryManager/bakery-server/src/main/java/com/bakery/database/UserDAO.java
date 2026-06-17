package com.bakery.database;

import com.bakery.shared.model.CartItem;
import com.bakery.shared.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.sql.SQLException;

public class UserDAO {

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE is_active = 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                User u = new User();
                u.setUsername(rs.getString("username"));
                u.setPassword(rs.getString("password_hash"));
                u.setRole(rs.getString("role"));
                u.setFullName(rs.getString("full_name"));
                u.setPhone(rs.getString("phone"));
                u.setBaseSalary(rs.getDouble("base_salary"));
                list.add(u);
            }
        } catch (Exception e) {
            System.out.println("[-] Lỗi SQL lấy User: " + e.getMessage());
        }
        return list;
    }

    public boolean addUser(User u) {
        String sql = "INSERT INTO users (username, password_hash, full_name, phone, base_salary, role, role_id, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, 1)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPassword());
            ps.setString(3, u.getFullName());
            ps.setString(4, u.getPhone());
            ps.setDouble(5, u.getBaseSalary());
            ps.setString(6, u.getRole());
            ps.setInt(7, "ADMIN".equals(u.getRole()) ? 1 : 2);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("[-] Lỗi SQL thêm User: " + e.getMessage());
            return false;
        }
    }

    public boolean updateUser(User u) {
        boolean hasPass = u.getPassword() != null && !u.getPassword().isEmpty();
        String sql = "UPDATE users SET full_name=?, phone=?, base_salary=?, role=?, role_id=? " +
                (hasPass ? ", password_hash=? " : "") + "WHERE username=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getPhone());
            ps.setDouble(3, u.getBaseSalary());
            ps.setString(4, u.getRole());
            ps.setInt(5, "ADMIN".equals(u.getRole()) ? 1 : 2);

            int index = 6;
            if (hasPass) { ps.setString(index++, u.getPassword()); }
            ps.setString(index, u.getUsername());

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("[-] Lỗi SQL sửa User: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteUser(String username) {
        String sql = "UPDATE users SET is_active = 0 WHERE username=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("[-] Lỗi SQL xóa User: " + e.getMessage());
            return false;
        }
    }

    public java.util.Map<String, Integer> authenticateUser(String username, String password) {
        String sql = "SELECT user_id, role_id FROM users WHERE username = ? AND password_hash = ? AND is_active = 1";
        try (java.sql.Connection conn = DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.util.Map<String, Integer> info = new java.util.HashMap<>();
                    info.put("roleId", rs.getInt("role_id"));
                    info.put("userId", rs.getInt("user_id"));
                    return info;
                }
            }
        } catch (Exception e) {
            System.out.println("[-] Lỗi SQL Đăng nhập: " + e.getMessage());
        }
        return null;
    }

    // --- BÁO CÁO CA LÀM HÔM NAY
    public String getShiftReportToday() {
        StringBuilder report = new StringBuilder();

        String sql = "SELECT u.full_name, COUNT(o.order_id) as total_orders, SUM(o.total_amount) as total_revenue " +
                "FROM orders o " +
                "JOIN users u ON o.user_id = u.user_id " +
                "WHERE CAST(o.created_at AS DATE) = CAST(GETDATE() AS DATE) " +
                "GROUP BY o.user_id, u.full_name";

        try (java.sql.Connection conn = DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {

            boolean hasData = false;
            while (rs.next()) {
                hasData = true;
                String cashierName = rs.getString("full_name");
                int orders = rs.getInt("total_orders");
                double rev = rs.getDouble("total_revenue");

                report.append("👤 Nhân viên trực: ").append(cashierName != null ? cashierName : "Không rõ").append("\n")
                        .append("   + Phục vụ: ").append(orders).append(" đơn hàng\n")
                        .append("   + Mang về: ").append(String.format("%,.0f", rev)).append(" VNĐ\n\n");
            }
            if (!hasData) {
                return "Hôm nay chưa có nhân viên nào chốt được đơn hàng cả!";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi truy xuất dữ liệu ca làm: " + e.getMessage();
        }
        return report.toString();
    }

    // --- LẤY DỮ LIỆU DASHBOARD
    public String getOrdersByUser(String username, String filter) {
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        com.google.gson.JsonArray historyArray = new com.google.gson.JsonArray();
        com.google.gson.JsonObject chartObj = new com.google.gson.JsonObject();

        String timeCondition = "1=1";
        if ("Hôm nay".equals(filter)) timeCondition = "CAST(o.created_at AS DATE) = CAST(GETDATE() AS DATE)";
        else if ("Tuần này".equals(filter)) timeCondition = "DATEDIFF(wk, o.created_at, GETDATE()) = 0";
        else if ("Tháng này".equals(filter)) timeCondition = "YEAR(o.created_at) = YEAR(GETDATE()) AND MONTH(o.created_at) = MONTH(GETDATE())";
        else if ("Năm nay".equals(filter)) timeCondition = "YEAR(o.created_at) = YEAR(GETDATE())";

        String sql = "SELECT o.order_id, o.created_at, o.total_amount, o.payment_method, o.customer_name " +
                "FROM orders o JOIN users u ON o.user_id = u.user_id " +
                "WHERE u.username = ? AND " + timeCondition + " ORDER BY o.created_at ASC";

        String detailSql = "SELECT p.product_name, od.quantity FROM order_details od JOIN products p ON od.product_id = p.product_id WHERE od.order_id = ?";

        int totalOrders = 0; double totalRevenue = 0;
        int cashCount = 0; int transferCount = 0;

        java.util.Map<String, Integer> productSales = new java.util.HashMap<>();
        java.util.Map<String, Integer> hourSales = new java.util.HashMap<>();

        try (java.sql.Connection conn = DatabaseConnection.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);
             java.sql.PreparedStatement pstmtDetail = conn.prepareStatement(detailSql)) {

            pstmt.setString(1, username);
            java.sql.ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                totalOrders++;
                String id = rs.getString("order_id");
                String fullTime = rs.getString("created_at");
                double amt = rs.getDouble("total_amount");
                String cusName = rs.getString("customer_name");
                boolean isCash = "CASH".equals(rs.getString("payment_method"));

                if (isCash) cashCount++; else transferCount++;
                totalRevenue += amt;

                String day = fullTime.split(" ")[0];
                String time = fullTime.split(" ")[1];

                String hour = time.split(":")[0] + ":00";
                hourSales.put(hour, hourSales.getOrDefault(hour, 0) + 1);

                double currentDayTotal = chartObj.has(day) ? chartObj.get(day).getAsDouble() : 0;
                chartObj.addProperty(day, currentDayTotal + amt);

                pstmtDetail.setString(1, id);
                java.sql.ResultSet rsDetail = pstmtDetail.executeQuery();
                StringBuilder detailStr = new StringBuilder();
                while(rsDetail.next()) {
                    String pName = rsDetail.getString("product_name");
                    int qty = rsDetail.getInt("quantity");

                    productSales.put(pName, productSales.getOrDefault(pName, 0) + qty);
                    detailStr.append(String.format(" + %sx %s\n", qty, pName));
                }
                rsDetail.close();

                com.google.gson.JsonObject orderObj = new com.google.gson.JsonObject();
                orderObj.addProperty("date", day);
                orderObj.addProperty("time", time);
                orderObj.addProperty("id", id);
                orderObj.addProperty("customer", (cusName == null || cusName.trim().isEmpty()) ? "Khách lẻ" : cusName);
                orderObj.addProperty("amount", amt);
                orderObj.addProperty("isCash", isCash);
                orderObj.addProperty("items", detailStr.toString().trim());

                historyArray.add(orderObj);
            }

            // Thuật toán sắp xếp Top 5 Sản phẩm bán chạy nhất
            com.google.gson.JsonArray topProductsArr = new com.google.gson.JsonArray();
            productSales.entrySet().stream()
                    .sorted(java.util.Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .forEach(e -> {
                        com.google.gson.JsonObject p = new com.google.gson.JsonObject();
                        p.addProperty("name", e.getKey());
                        p.addProperty("qty", e.getValue());
                        topProductsArr.add(p);
                    });

            // Thuật toán sắp xếp Top 3 Khung giờ vàng
            com.google.gson.JsonArray topHoursArr = new com.google.gson.JsonArray();
            hourSales.entrySet().stream()
                    .sorted(java.util.Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(3)
                    .forEach(e -> {
                        com.google.gson.JsonObject h = new com.google.gson.JsonObject();
                        h.addProperty("hour", e.getKey());
                        h.addProperty("count", e.getValue());
                        topHoursArr.add(h);
                    });

            root.addProperty("totalOrders", totalOrders);
            root.addProperty("totalRevenue", totalRevenue);
            root.addProperty("cashCount", cashCount);
            root.addProperty("transferCount", transferCount);
            root.add("chartData", chartObj);
            root.add("history", historyArray);
            root.add("topProducts", topProductsArr);
            root.add("topHours", topHoursArr);

            return root.toString();

        } catch (Exception e) {
            e.printStackTrace();
            com.google.gson.JsonObject err = new com.google.gson.JsonObject();
            err.addProperty("error", e.getMessage());
            return err.toString();
        }
    }
}