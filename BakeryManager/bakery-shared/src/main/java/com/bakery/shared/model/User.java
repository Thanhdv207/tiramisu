package com.bakery.shared.model;

public class User {
    private String username;
    private String password;
    private String role;
    private String fullName;
    private String phone;
    private double baseSalary;

    // --- Constructor rỗng (Bắt buộc phải có để thằng Gson nó dịch JSON) ---
    public User() {
    }

    // --- Constructor đầy đủ tham số (Dùng lúc tạo nhân viên mới trên giao diện) ---
    public User(String username, String password, String role, String fullName, String phone, double baseSalary) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.fullName = fullName;
        this.phone = phone;
        this.baseSalary = baseSalary;
    }

    // ==========================================
    // GETTER VÀ SETTER
    // ==========================================
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public double getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(double baseSalary) {
        this.baseSalary = baseSalary;
    }
}