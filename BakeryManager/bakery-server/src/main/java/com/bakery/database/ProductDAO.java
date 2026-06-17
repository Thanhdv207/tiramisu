package com.bakery.database;

import com.bakery.shared.model.Product;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                products.add(new Product(
                        rs.getString("product_id"),
                        rs.getString("product_name"),
                        rs.getInt("category_id"),
                        rs.getDouble("price"),
                        rs.getInt("stock_quantity")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    // Cập nhật tồn kho an toàn chống SQL Injection
    public boolean updateStock(String productId, int quantityToReduce) {
        String sql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ? AND stock_quantity >= ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, quantityToReduce);
            pstmt.setString(2, productId);
            pstmt.setInt(3, quantityToReduce); // Đảm bảo không bị âm kho

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}