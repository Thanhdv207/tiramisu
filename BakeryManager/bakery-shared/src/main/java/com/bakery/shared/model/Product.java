package com.bakery.shared.model;

public class Product {
    private String productId;
    private String productName;
    private int categoryId;
    private double price;
    private int stockQuantity;

    public Product(String productId, String productName, int categoryId, double price, int stockQuantity) {
        this.productId = productId;
        this.productName = productName;
        this.categoryId = categoryId;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getCategoryId() { return categoryId; }
    public double getPrice() { return price; }
    public int getStockQuantity() { return stockQuantity; }
}