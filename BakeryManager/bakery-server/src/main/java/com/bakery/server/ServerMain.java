package com.bakery.server;

import com.bakery.database.AdminDAO;
import com.bakery.database.OrderDAO;
import com.bakery.database.UserDAO;
import com.bakery.shared.model.CartItem;
import com.bakery.shared.model.Order;
import com.bakery.shared.model.Product;
import com.bakery.shared.model.User;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerMain {
    private static final int PORT = 8888;
    public static List<String> chatHistory = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("====== BAKERY TCP/IP SERVER IS STARTING ======");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[+] Server is listening on port " + PORT + "...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[+] New client connected: " + clientSocket.getInetAddress().getHostAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (Exception e) {
            System.err.println("[-] Server Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private Gson gson = new Gson();
        private UserDAO userDAO = new UserDAO();
        private AdminDAO adminDAO = new AdminDAO();
        private OrderDAO orderDAO = new OrderDAO();

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                String jsonRequest = in.readLine();
                if (jsonRequest != null) {
                    System.out.println(">>> Received from client: " + jsonRequest);
                    Map<String, Object> request = gson.fromJson(jsonRequest, Map.class);
                    String action = (String) request.get("action");
                    Object responseData = processAction(action, request);
                    String jsonResponse = gson.toJson(responseData);
                    out.println(jsonResponse);
                    System.out.println("<<< Sent to client: " + jsonResponse);
                }

            } catch (Exception e) {
                System.err.println("[-] Client processing error: " + e.getMessage());
            } finally {
                try { socket.close(); } catch (Exception e) { e.printStackTrace(); }
            }
        }

        private Object processAction(String action, Map<String, Object> request) {
            Map<String, Object> response = new HashMap<>();
            try {
                switch (action) {
                    case "SEND_CHAT":
                        String sender = (String) request.get("sender");
                        String msg = (String) request.get("message");
                        chatHistory.add(sender + ": " + msg);
                        response.put("status", "SUCCESS");
                        return response;

                    case "GET_CHAT":
                        return chatHistory;

                    case "LOGIN":
                        String username = (String) request.get("username");
                        String password = (String) request.get("password");

                        java.util.Map<String, Integer> authInfo = userDAO.authenticateUser(username, password);

                        if (authInfo != null) {
                            response.put("status", "SUCCESS");
                            response.put("roleId", authInfo.get("roleId"));
                            response.put("userId", authInfo.get("userId"));
                        } else {
                            response.put("status", "FAIL");
                        }
                        return response;

                    case "GET_PRODUCTS":
                        return adminDAO.getAllProducts();

                    // ---> ĐÃ NÂNG CẤP BỌC THÉP CHỖ NÀY <---
                    case "CREATE_ORDER":
                        try {
                            // Ép kiểu siêu an toàn, chống mọi loại lỗi ClassCastException của Gson
                            double total = Double.parseDouble(String.valueOf(request.get("total")));
                            String cusName = String.valueOf(request.get("customerName"));
                            String cusPhone = String.valueOf(request.get("customerPhone"));
                            int userId = (int) Double.parseDouble(String.valueOf(request.get("userId")));
                            String paymentMethod = String.valueOf(request.get("paymentMethod"));

                            String cartJson = gson.toJson(request.get("cartItems"));
                            Type listType = new TypeToken<List<CartItem>>(){}.getType();
                            List<CartItem> cartItems = gson.fromJson(cartJson, listType);

                            boolean isSuccess = orderDAO.createOrder(cartItems, total, userId, cusName, cusPhone, paymentMethod);

                            if (isSuccess) {
                                response.put("status", "SUCCESS");
                            } else {
                                response.put("status", "FAIL");
                                response.put("message", "Cột payment_method trong DB đang từ chối chữ '" + paymentMethod + "'. Hãy mở MySQL đổi loại cột đó thành VARCHAR(50) nhé!");
                            }
                        } catch (Exception ex) {
                            response.put("status", "FAIL");
                            response.put("message", "Lỗi Server phân tích dữ liệu: " + ex.getMessage());
                        }
                        return response;

                    case "GET_REVENUE":
                        String filterRev = (String) request.get("filter");
                        return String.valueOf(adminDAO.getRevenue(filterRev));

                    case "GET_TOP_PRODUCTS":
                        String filterTop = (String) request.get("filter");
                        return adminDAO.getTopSellingProducts(filterTop);

                    case "GET_CHART_DATA":
                        String filterChart = (String) request.get("filter");
                        return adminDAO.getChartData(filterChart);

                    case "ADD_PRODUCT":
                        Product pAdd = gson.fromJson(gson.toJson(request.get("product")), Product.class);
                        response.put("status", adminDAO.addProduct(pAdd) ? "SUCCESS" : "FAIL");
                        return response;

                    case "UPDATE_PRODUCT":
                        Product pUpdate = gson.fromJson(gson.toJson(request.get("product")), Product.class);
                        response.put("status", adminDAO.updateProduct(pUpdate) ? "SUCCESS" : "FAIL");
                        return response;

                    case "DELETE_PRODUCT":
                        String delId = (String) request.get("productId");
                        response.put("status", adminDAO.deleteProduct(delId) ? "SUCCESS" : "FAIL");
                        return response;

                    case "GET_ORDERS":
                        String filterOrder = (String) request.get("filter");
                        return adminDAO.getAllOrders(filterOrder);

                    case "GET_ORDER_DETAILS":
                        String orderId = (String) request.get("orderId");
                        return adminDAO.getOrderDetails(orderId);

                    case "GET_ALL_USERS":
                        return userDAO.getAllUsers();

                    case "ADD_USER":
                        User uAdd = gson.fromJson(gson.toJson(request.get("user")), User.class);
                        response.put("status", userDAO.addUser(uAdd) ? "SUCCESS" : "FAIL");
                        return response;

                    case "UPDATE_USER":
                        User uUpd = gson.fromJson(gson.toJson(request.get("user")), User.class);
                        response.put("status", userDAO.updateUser(uUpd) ? "SUCCESS" : "FAIL");
                        return response;

                    case "DELETE_USER":
                        String uDel = (String) request.get("username");
                        response.put("status", userDAO.deleteUser(uDel) ? "SUCCESS" : "FAIL");
                        return response;

                    case "GET_SHIFT_REPORT":
                        response.put("status", "SUCCESS");
                        response.put("data", userDAO.getShiftReportToday());
                        return response;

                    case "GET_ORDERS_BY_USER":
                        String targetUser = (String) request.get("username");
                        // Lấy thêm bộ lọc, nếu client không gửi thì mặc định là "Tháng này"
                        String filter = request.containsKey("filter") ? (String) request.get("filter") : "Tháng này";

                        response.put("status", "SUCCESS");
                        response.put("data", userDAO.getOrdersByUser(targetUser, filter));
                        return response;                    default:
                        response.put("status", "UNKNOWN_ACTION");
                        return response;
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.put("status", "ERROR");
                response.put("message", e.getMessage());
                return response;
            }
        }
    }
}