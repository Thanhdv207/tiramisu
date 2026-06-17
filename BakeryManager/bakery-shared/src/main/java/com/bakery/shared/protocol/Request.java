package com.bakery.shared.protocol;

public class Request {
    private String action; // Ví dụ: "LOGIN", "GET_PRODUCTS", "CREATE_ORDER"
    private String payload; // Dữ liệu đính kèm (Chuỗi JSON)

    public Request(String action, String payload) {
        this.action = action;
        this.payload = payload;
    }

    public String getAction() { return action; }
    public String getPayload() { return payload; }
}