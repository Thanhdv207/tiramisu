package com.bakery.server.core;

import com.bakery.database.UserDAO;
import com.bakery.database.AdminDAO;
import com.bakery.shared.model.User;
import com.bakery.shared.protocol.Request;
import com.bakery.shared.protocol.Response;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Gson gson = new Gson();

    // Nơi lưu trữ Bong Bóng Chat (Lưu tạm trên RAM Server)
    private static List<String> chatHistory = new ArrayList<>();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("<- Received: " + inputLine);
                try {
                    Request request = gson.fromJson(inputLine, Request.class);
                    // Xử lý và phân luồng
                    Response response = processRequest(request, inputLine);

                    // CHỈ gửi Response object nếu có (Các hàm trả data thô sẽ return null)
                    if (response != null) {
                        String jsonResponse = gson.toJson(response);
                        System.out.println("-> Send: " + jsonResponse);
                        out.println(jsonResponse);
                    }
                } catch (Exception e) {
                    System.out.println("[-] Lỗi định dạng Request: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("[-] Client Disconnected.");
        }
    }

    private Response processRequest(Request req, String rawJson) {
        try {
            UserDAO userDAO = new UserDAO();
            AdminDAO adminDAO = new AdminDAO();
            JsonObject jsonObj = gson.fromJson(rawJson, JsonObject.class);
            String action = req.getAction();

            switch (action) {
                // =========================================================
                // 1. NHÓM TRẢ VỀ CLASS `RESPONSE` (Cấu trúc: action, status, data)
                // =========================================================
                case "GET_GLOBAL_DASHBOARD":
                    // Lấy filter từ jsonObj
                    String filterDash = "Hôm nay";
                    if (jsonObj != null && jsonObj.has("filter") && !jsonObj.get("filter").isJsonNull()) {
                        filterDash = jsonObj.get("filter").getAsString();
                    }

                    // Gọi thẳng AdminDAO
                    com.bakery.database.AdminDAO adminDaoForDash = new com.bakery.database.AdminDAO();
                    String globalRes = adminDaoForDash.getGlobalAnalytics(filterDash);

                    // Bắn thẳng dữ liệu thô về Client, không bọc qua Response nữa!
                    out.println(globalRes);
                    return null;
                case "GET_ORDERS_BY_USER":
                    String username = jsonObj.get("username").getAsString();
                    String uFilter = jsonObj.has("filter") ? jsonObj.get("filter").getAsString() : "Hôm nay";
                    String userRes = userDAO.getOrdersByUser(username, uFilter);
                    return new Response("GET_ORDERS_BY_USER", "SUCCESS", userRes);

                case "GET_ALL_USERS":
                    List<User> users = userDAO.getAllUsers();
                    return new Response("GET_ALL_USERS", "SUCCESS", gson.toJson(users));

                case "ADD_USER":
                    User newUser = gson.fromJson(jsonObj.get("user"), User.class);
                    boolean isAdded = userDAO.addUser(newUser);
                    return new Response("ADD_USER", isAdded ? "SUCCESS" : "ERROR", "");

                case "UPDATE_USER":
                    User updUser = gson.fromJson(jsonObj.get("user"), User.class);
                    boolean isUpd = userDAO.updateUser(updUser);
                    return new Response("UPDATE_USER", isUpd ? "SUCCESS" : "ERROR", "");

                case "DELETE_USER":
                    String delUser = jsonObj.get("username").getAsString();
                    boolean isDel = userDAO.deleteUser(delUser);
                    return new Response("DELETE_USER", isDel ? "SUCCESS" : "ERROR", "");

                case "ADD_PRODUCT":
                    com.bakery.shared.model.Product newProd = gson.fromJson(jsonObj.get("product"), com.bakery.shared.model.Product.class);
                    boolean pAdded = adminDAO.addProduct(newProd);
                    return new Response("ADD_PRODUCT", pAdded ? "SUCCESS" : "ERROR", "");

                case "UPDATE_PRODUCT":
                    com.bakery.shared.model.Product updProd = gson.fromJson(jsonObj.get("product"), com.bakery.shared.model.Product.class);
                    boolean pUpd = adminDAO.updateProduct(updProd);
                    return new Response("UPDATE_PRODUCT", pUpd ? "SUCCESS" : "ERROR", "");

                case "DELETE_PRODUCT":
                    String delProdId = jsonObj.get("productId").getAsString();
                    boolean pDel = adminDAO.deleteProduct(delProdId);
                    return new Response("DELETE_PRODUCT", pDel ? "SUCCESS" : "ERROR", "");

                case "LOGIN":
                    return new Response("LOGIN", "SUCCESS", "{\"role\":\"ADMIN\"}");

                // =========================================================
                // 2. NHÓM TRẢ VỀ DỮ LIỆU THÔ (Không bọc bằng Response)
                //    Khắc phục tận gốc lỗi Trắng Dashboard!
                // =========================================================
                case "GET_REVENUE":
                    double rev = adminDAO.getRevenue(jsonObj.has("filter") ? jsonObj.get("filter").getAsString() : "Hôm nay");
                    out.println(rev);
                    return null;

                case "GET_TOP_PRODUCTS":
                    java.util.Map<String, Integer> top = adminDAO.getTopSellingProducts(jsonObj.has("filter") ? jsonObj.get("filter").getAsString() : "Hôm nay");
                    out.println(gson.toJson(top));
                    return null;

                case "GET_CHART_DATA":
                    java.util.Map<String, Double> chart = adminDAO.getChartData(jsonObj.has("filter") ? jsonObj.get("filter").getAsString() : "Hôm nay");
                    out.println(gson.toJson(chart));
                    return null;

                case "GET_PRODUCTS":
                    java.util.List<com.bakery.shared.model.Product> prods = adminDAO.getAllProducts();
                    out.println(gson.toJson(prods));
                    return null;

                case "GET_ORDERS":
                    String ordFilter = jsonObj.has("filter") ? jsonObj.get("filter").getAsString() : "Hôm nay";
                    java.util.List<com.bakery.shared.model.Order> ords = adminDAO.getAllOrders(ordFilter);
                    out.println(gson.toJson(ords));
                    return null;

                case "GET_ORDER_DETAILS":
                    String orderId = jsonObj.get("orderId").getAsString();
                    java.util.List<com.bakery.shared.model.CartItem> details = adminDAO.getOrderDetails(orderId);
                    out.println(gson.toJson(details));
                    return null;

                // =========================================================
                // 3. XỬ LÝ KHUNG CHAT MESSENGER NỔI
                // =========================================================
                case "GET_CHAT":
                    out.println(gson.toJson(chatHistory));
                    return null;

                case "SEND_CHAT":
                    String sender = jsonObj.has("sender") ? jsonObj.get("sender").getAsString() : "Ẩn danh";
                    String message = jsonObj.has("message") ? jsonObj.get("message").getAsString() : "";
                    chatHistory.add(sender + ": " + message);
                    out.println("{\"status\":\"SUCCESS\"}");
                    return null;

                default:
                    return new Response("ERROR", "ERROR", "Lệnh không tồn tại: " + action);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Response("ERROR", "ERROR", "Lỗi Server: " + e.getMessage());
        }
    }
}