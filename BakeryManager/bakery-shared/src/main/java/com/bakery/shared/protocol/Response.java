package com.bakery.shared.protocol;

public class Response {
    private String status;  // "SUCCESS" hoặc "ERROR"
    private String message; // Thông báo cho người dùng
    private String data;    // Dữ liệu thực tế (Chuỗi JSON)

    public Response(String status, String message, String data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public String getData() { return data; }
}