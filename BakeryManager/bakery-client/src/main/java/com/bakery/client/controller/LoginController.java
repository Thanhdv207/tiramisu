package com.bakery.client.controller;

import com.google.gson.Gson;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class LoginController {

    // --- LƯU THÔNG TIN NGƯỜI DÙNG ĐANG ĐĂNG NHẬP (Làm "thẻ căn cước" dùng chung cho toàn App) ---
    public static int currentUserId = -1;
    public static String currentUsername = "";
    public static int currentRoleId = -1;

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;
    @FXML private Button btnLogin;

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();

        // Validate dữ liệu trống
        if (username.isEmpty() || password.isEmpty()) {
            lblError.setText("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
            return;
        }

        try (Socket socket = new Socket("localhost", 8888);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))) {

            // 1. Đóng gói thông tin đăng nhập thành JSON
            Map<String, String> request = new HashMap<>();
            request.put("action", "LOGIN");
            request.put("username", username);
            request.put("password", password);

            Gson gson = new Gson();
            out.println(gson.toJson(request)); // Gửi qua Server

            // 2. Chờ Server check Database và trả lời
            String responseJson = in.readLine();
            Map<String, Object> response = gson.fromJson(responseJson, Map.class);

            // 3. Xử lý kết quả trả về
            if (response != null && "SUCCESS".equals(response.get("status"))) {
                lblError.setText("");
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng nhập thành công!");

                // Lấy Role ID
                int roleId = 2;
                if (response.get("roleId") != null) {
                    roleId = ((Double) response.get("roleId")).intValue();
                }

                // ---> QUAN TRỌNG: LẤY MÃ ID THẬT CỦA NHÂN VIÊN VÀ LƯU VÀO BỘ NHỚ TẠM <---
                if (response.get("userId") != null) {
                    currentUserId = ((Double) response.get("userId")).intValue();
                } else {
                    currentUserId = 1; // Fallback phòng hờ Server chưa cập nhật kịp
                }
                currentUsername = username;
                currentRoleId = roleId;

                // Xử lý phân quyền cơ bản
                if (roleId == 1) {
                    // Chuyển sang màn hình ADMIN
                    try {
                        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/bakery/view/AdminDashboard.fxml"));
                        javafx.scene.Parent root = loader.load();
                        javafx.stage.Stage stage = (javafx.stage.Stage) btnLogin.getScene().getWindow();
                        stage.setScene(new javafx.scene.Scene(root, 1024, 768));
                        stage.setTitle("Bakery Manager - Admin Dashboard (" + username + ")");
                        stage.setResizable(true);
                        stage.centerOnScreen();
                        stage.show();
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải giao diện Admin!");
                    }
                } else if (roleId == 2) {
                    // Chuyển hướng sang giao diện Bán Hàng (POS)
                    try {
                        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/bakery/view/POSView.fxml"));
                        javafx.scene.Parent posRoot = loader.load();
                        javafx.stage.Stage stage = (javafx.stage.Stage) btnLogin.getScene().getWindow();
                        stage.setScene(new javafx.scene.Scene(posRoot, 1024, 768));
                        stage.setTitle("Bakery Manager - Bán Hàng (" + username + ")");
                        stage.setResizable(true);
                        stage.centerOnScreen();
                        stage.show();
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải giao diện POS!");
                    }
                }
            } else {
                lblError.setText("Tên đăng nhập hoặc mật khẩu không đúng!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            lblError.setText("Lỗi: Không thể kết nối đến Server (8888)!");
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}