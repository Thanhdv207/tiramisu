package com.bakery.shared.model;
public class CartItem {
    private String productId;
    private String productName;
    private int quantity;
    private double unitPrice;

    public CartItem(String productId, String productName, int quantity, double unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    // Các hàm Getters là BẮT BUỘC để JavaFX TableView có thể đọc được dữ liệu
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }

    // Tổng tiền của dòng này = Số lượng * Đơn giá
    public double getTotal() { return quantity * unitPrice; }

    public void addQuantity(int amount) {
        this.quantity += amount;
    }
}