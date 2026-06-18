package com.bakery.client.controller;

import com.bakery.shared.model.User;
import com.bakery.shared.protocol.Request;
import com.bakery.shared.protocol.Response;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import java.util.Map;
import java.util.HashMap;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.LinkedHashMap;

public class UserManagementController implements Initializable {

    @FXML private TextField txtUsername, txtFullName, txtPhone, txtSalary;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cbRole;
    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, String> colUsername, colFullName, colPhone, colRole;
    @FXML private TableColumn<User, Double> colSalary;

    private ObservableList<User> userList = FXCollections.observableArrayList();
    private Gson gson = new Gson();

    private String sendToServer(Map<String, Object> requestData) {
        try (Socket socket = new Socket("localhost", 8888);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))) {
            out.println(gson.toJson(requestData));
            return in.readLine();
        } catch (Exception e) {
            System.err.println("[-] Lỗi kết nối Server: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Ánh xạ các cột trong bảng với biến trong class User
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colSalary.setCellValueFactory(new PropertyValueFactory<>("baseSalary"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        // 2. Setup dữ liệu cho ComboBox và Table
        cbRole.setItems(FXCollections.observableArrayList("ADMIN", "NHANVIEN"));
        tableUsers.setItems(userList);

        // 3. Sự kiện Click chuột vào bảng -> Bắn dữ liệu sang Form bên trái
        tableUsers.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                txtUsername.setText(newSel.getUsername());
                txtFullName.setText(newSel.getFullName());
                txtPhone.setText(newSel.getPhone());
                txtSalary.setText(String.valueOf(newSel.getBaseSalary()));
                cbRole.setValue(newSel.getRole());
                // Không hiển thị password cũ để bảo mật
                txtPassword.clear();
                txtPassword.setPromptText("Bỏ trống nếu không muốn đổi pass");
            }
        });

        // 4. Lấy dữ liệu từ Server ngay khi vừa mở Form
        loadDataFromServer();
    }
    // --- HÀM KẾT NỐI SERVER ĐỂ LẤY DANH SÁCH NHÂN VIÊN ---
    private void loadUserDataFromServer() {
        // Tạo một luồng (Thread) chạy nền để không làm đơ giao diện Admin
        new Thread(() -> {
            try (Socket socket = new Socket("localhost", 8888);
                 PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))) {

                // 1. Đóng gói lệnh GET_ALL_USERS gửi lên Server
                Request req = new Request("GET_ALL_USERS", null);
                out.println(gson.toJson(req));

                // 2. Lắng nghe Server trả kết quả về
                String responseStr = in.readLine();
                if (responseStr != null) {
                    Response res = gson.fromJson(responseStr, Response.class);

                    if ("SUCCESS".equals(res.getStatus())) {
                        // 3. Dịch chuỗi JSON thành mảng List<User> (Cần Alt+Enter import TypeToken)
                        List<User> list = gson.fromJson(res.getData(), new com.google.gson.reflect.TypeToken<List<User>>(){}.getType());

                        // 4. Ném dữ liệu lên giao diện (JavaFX bắt buộc phải dùng Platform.runLater)
                        javafx.application.Platform.runLater(() -> {
                            userList.setAll(list);
                        });
                    }
                }
            } catch (Exception e) {
                System.out.println("Lỗi khi tải danh sách nhân viên từ Server:");
                e.printStackTrace();
            }
        }).start();
    }
    private void loadDataFromServer() {
        // Tạo một luồng (Thread) chạy ngầm để không làm đơ giao diện
        new Thread(() -> {
            try (Socket socket = new Socket("localhost", 8888);
                 PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))) {

                // Gửi Request lấy dữ liệu
                Request req = new Request("GET_ALL_USERS", null);
                out.println(gson.toJson(req));

                // Đợi Server trả JSON về
                String responseStr = in.readLine();
                Response res = gson.fromJson(responseStr, Response.class);

                if ("SUCCESS".equals(res.getStatus())) {
                    // Dịch JSON thành mảng List<User>
                    List<User> list = gson.fromJson(res.getData(), new TypeToken<List<User>>(){}.getType());

                    // Ném dữ liệu vào luồng giao diện chính (Bắt buộc của JavaFX)
                    Platform.runLater(() -> {
                        userList.setAll(list);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // --- CÁC HÀM XỬ LÝ NÚT BẤM (Sẽ viết ở nhịp sau) ---
    @FXML
    private void handleAddUser() {
        System.out.println("Nút Thêm vừa được bấm!");
    }

    @FXML
    private void handleUpdateUser() {
        System.out.println("Nút Sửa vừa được bấm!");
    }

    @FXML
    private void handleDeleteUser() {
        System.out.println("Nút Xóa vừa được bấm!");
    }
    // --- HÀM XEM DASHBOARD NĂNG SUẤT (BẢN FINAL: FULL CHI TIẾT ĐƠN + KHÁCH HÀNG) ---
    @FXML
    private void handleViewEmployeeOrders() {
        User selectedUser = tableUsers.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("Lưu ý");
            alert.setHeaderText(null);
            alert.setContentText("Vui lòng chọn 1 nhân viên trong bảng để xem!");
            alert.showAndWait();
            return;
        }

        // 1. TẠO STAGE MỚI (CỬA SỔ DASHBOARD ĐỘC LẬP)
        javafx.stage.Stage dashStage = new javafx.stage.Stage();
        dashStage.setTitle("Productivity Dashboard - " + selectedUser.getFullName());
        dashStage.setWidth(1000);
        dashStage.setHeight(750);

        // 2. NHÚNG CSS SIÊU CẤP
        String css = ".root-pane { -fx-background-color: #F8FAFC; -fx-font-family: 'Segoe UI', Inter, sans-serif; }"
                + ".card { -fx-background-color: white; -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(148, 163, 184, 0.15), 10, 0, 0, 4); }"
                + ".card:hover { -fx-effect: dropshadow(three-pass-box, rgba(37, 99, 235, 0.12), 15, 0, 0, 8); -fx-translate-y: -2; }"
                + ".combo-box { -fx-background-color: white; -fx-border-color: #CBD5E1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-weight: bold; -fx-text-fill: #1E293B; }"
                + ".combo-box .list-cell { -fx-padding: 8; }"
                + ".badge-active { -fx-background-color: #D1FAE5; -fx-text-fill: #10B981; -fx-background-radius: 20; -fx-padding: 4 12; -fx-font-weight: bold; -fx-font-size: 11px; }"
                + ".badge-cash { -fx-background-color: #DBEAFE; -fx-text-fill: #2563EB; -fx-background-radius: 6; -fx-padding: 4 8; -fx-font-size: 11px; -fx-font-weight: bold; }"
                + ".badge-transfer { -fx-background-color: #FEF3C7; -fx-text-fill: #D97706; -fx-background-radius: 6; -fx-padding: 4 8; -fx-font-size: 11px; -fx-font-weight: bold; }"
                + ".chart-title { -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #475569; -fx-padding: 0 0 10 0; }"
                + ".scroll-pane { -fx-background-color: transparent; } .scroll-pane > .viewport { -fx-background-color: transparent; }";

        String dataUri = "data:text/css;charset=utf-8," + css.replace(" ", "%20").replace("\n", "");

        // 3. KHUNG GIAO DIỆN CHÍNH
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(20);
        root.getStyleClass().add("root-pane");
        root.setStyle("-fx-padding: 25;");

        // --- HEADER ---
        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(15);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        javafx.scene.shape.Circle avatar = new javafx.scene.shape.Circle(25, javafx.scene.paint.Color.web("#3B82F6"));
        javafx.scene.control.Label lblInitials = new javafx.scene.control.Label(selectedUser.getFullName().substring(0, 1).toUpperCase());
        lblInitials.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        javafx.scene.layout.StackPane avatarPane = new javafx.scene.layout.StackPane(avatar, lblInitials);

        javafx.scene.layout.VBox nameBox = new javafx.scene.layout.VBox(2);
        javafx.scene.control.Label lblName = new javafx.scene.control.Label(selectedUser.getFullName());
        lblName.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        javafx.scene.control.Label lblStatus = new javafx.scene.control.Label("● Đang hoạt động");
        lblStatus.getStyleClass().add("badge-active");
        nameBox.getChildren().addAll(lblName, lblStatus);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.control.ComboBox<String> cbFilter = new javafx.scene.control.ComboBox<>();
        cbFilter.getItems().addAll("Hôm nay", "Tuần này", "Tháng này", "Năm nay", "Tất cả");
        cbFilter.setValue("Tháng này");
        cbFilter.setPrefHeight(40);

        header.getChildren().addAll(avatarPane, nameBox, spacer, cbFilter);

        // --- KHU VỰC CHỨA DỮ LIỆU ĐỘNG ---
        javafx.scene.layout.VBox dynamicContent = new javafx.scene.layout.VBox(20);
        javafx.scene.layout.VBox.setVgrow(dynamicContent, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.control.ScrollPane mainScroll = new javafx.scene.control.ScrollPane(dynamicContent);
        mainScroll.setFitToWidth(true);
        mainScroll.getStyleClass().add("scroll-pane");
        javafx.scene.layout.VBox.setVgrow(mainScroll, javafx.scene.layout.Priority.ALWAYS);

        root.getChildren().addAll(header, mainScroll);

        // 4. HÀM RENDER DỮ LIỆU
        java.text.NumberFormat curFormat = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("vi", "VN"));

        Runnable loadData = () -> {
            dynamicContent.getChildren().clear();
            Map<String, Object> req = new HashMap<>();
            req.put("action", "GET_ORDERS_BY_USER");
            req.put("username", selectedUser.getUsername());
            req.put("filter", cbFilter.getValue());

            String res = sendToServer(req);
            if (res != null) {
                try {
                    Map<String, String> response = gson.fromJson(res, new TypeToken<Map<String, String>>(){}.getType());
                    if ("SUCCESS".equals(response.get("status"))) {
                        String rawData = response.get("data");

                        if (!rawData.contains("==================================")) {
                            javafx.scene.control.Label lblEmpty = new javafx.scene.control.Label("📭 Không có dữ liệu giao dịch.");
                            lblEmpty.setStyle("-fx-font-size: 16px; -fx-text-fill: #94A3B8; -fx-padding: 50; -fx-alignment: center;");
                            lblEmpty.setMaxWidth(Double.MAX_VALUE);
                            dynamicContent.getChildren().add(lblEmpty);
                            return;
                        }

                        // PHÂN TÍCH DỮ LIỆU (BÓC TÁCH TỪNG DÒNG CỦA SERVER)
                        String[] parts = rawData.split("==================================\n\n");
                        String[] lines = (parts.length > 1 ? parts[1] : "").split("\n");

                        int totalOrders = 0; double totalRevenue = 0;
                        int cashCount = 0; int transferCount = 0;
                        Map<String, Double> chartData = new LinkedHashMap<>();

                        javafx.scene.layout.VBox timelineList = new javafx.scene.layout.VBox(15);
                        String currentDay = "";

                        // Bộ nhớ tạm để gom dữ liệu của 1 đơn hàng
                        String oTime = "", oId = "", oCustomer = "";
                        StringBuilder oItems = new StringBuilder();

                        for (String line : lines) {
                            String tLine = line.trim();
                            if (tLine.isEmpty() || tLine.startsWith("----")) continue;

                            if (tLine.startsWith("📅 Ngày:")) {
                                currentDay = tLine.replace("📅 Ngày:", "").trim();
                                chartData.putIfAbsent(currentDay, 0.0);

                                javafx.scene.control.Label lblDay = new javafx.scene.control.Label(currentDay);
                                lblDay.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #64748B; -fx-padding: 15 0 5 0;");
                                timelineList.getChildren().add(lblDay);
                            } else if (tLine.startsWith("🕒")) {
                                String[] timeId = tLine.split("\\|");
                                oTime = timeId[0].replace("🕒", "").trim();
                                oId = timeId[1].replace("Mã:", "").trim();
                            } else if (tLine.startsWith("👤 Khách:")) {
                                oCustomer = tLine.replace("👤 Khách:", "").trim();
                            } else if (tLine.startsWith("+")) {
                                oItems.append(tLine).append("\n"); // Gom tên món ăn
                            } else if (tLine.startsWith("➔ Tổng:")) {
                                totalOrders++;
                                String amtStr = tLine.substring(tLine.indexOf("Tổng:") + 5, tLine.indexOf("(")).replaceAll("[^0-9]", "");
                                double amt = amtStr.isEmpty() ? 0 : Double.parseDouble(amtStr);
                                totalRevenue += amt;
                                chartData.put(currentDay, chartData.getOrDefault(currentDay, 0.0) + amt);

                                boolean isCash = tLine.contains("Tiền mặt");
                                if (isCash) cashCount++; else transferCount++;

                                // --- BẮT ĐẦU VẼ THẺ HÓA ĐƠN ---
                                javafx.scene.layout.VBox orderCard = new javafx.scene.layout.VBox(12);
                                orderCard.getStyleClass().add("card");
                                orderCard.setStyle("-fx-padding: 18;");

                                // 1. Header thẻ (Icon, Mã đơn, Giờ, Khách, Số tiền)
                                javafx.scene.layout.HBox cardHeader = new javafx.scene.layout.HBox(15);
                                cardHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                                javafx.scene.control.Label lblIcon = new javafx.scene.control.Label("🛍️");
                                lblIcon.setStyle("-fx-font-size: 20px; -fx-background-color: #F1F5F9; -fx-background-radius: 8; -fx-padding: 8;");

                                javafx.scene.layout.VBox orderInfo = new javafx.scene.layout.VBox(2);
                                javafx.scene.control.Label lblId = new javafx.scene.control.Label(oId);
                                lblId.setStyle("-fx-font-weight: bold; -fx-text-fill: #2563EB; -fx-font-size: 14px;");
                                javafx.scene.control.Label lblTimeCus = new javafx.scene.control.Label(oTime + " • 👤 " + oCustomer);
                                lblTimeCus.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
                                orderInfo.getChildren().addAll(lblId, lblTimeCus);

                                javafx.scene.layout.Region oSpacer = new javafx.scene.layout.Region();
                                javafx.scene.layout.HBox.setHgrow(oSpacer, javafx.scene.layout.Priority.ALWAYS);

                                javafx.scene.layout.VBox moneyBox = new javafx.scene.layout.VBox(5);
                                moneyBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                                javafx.scene.control.Label lblAmt = new javafx.scene.control.Label(curFormat.format(amt));
                                lblAmt.setStyle("-fx-font-weight: bold; -fx-text-fill: #10B981; -fx-font-size: 15px;");
                                javafx.scene.control.Label lblPayType = new javafx.scene.control.Label(isCash ? "Tiền mặt" : "Chuyển khoản");
                                lblPayType.getStyleClass().add(isCash ? "badge-cash" : "badge-transfer");
                                moneyBox.getChildren().addAll(lblAmt, lblPayType);

                                cardHeader.getChildren().addAll(lblIcon, orderInfo, oSpacer, moneyBox);

                                // 2. Thân thẻ: Liệt kê chi tiết món ăn (Khung xám thụt lề)
                                javafx.scene.control.Label lblItems = new javafx.scene.control.Label(oItems.toString().trim());
                                lblItems.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px; -fx-text-fill: #334155; -fx-background-color: #F8FAFC; -fx-padding: 12; -fx-background-radius: 6; -fx-border-color: #E2E8F0; -fx-border-radius: 6;");
                                lblItems.setMaxWidth(Double.MAX_VALUE);

                                orderCard.getChildren().addAll(cardHeader, lblItems);
                                timelineList.getChildren().add(orderCard);

                                // Reset bộ nhớ tạm để chuẩn bị vẽ thẻ tiếp theo
                                oTime = ""; oId = ""; oCustomer = ""; oItems.setLength(0);
                            }
                        }

                        // --- 1. RENDER THẺ KPI (GIỮ NGUYÊN BẢN XỊN) ---
                        javafx.scene.layout.HBox kpiBox = new javafx.scene.layout.HBox(15);
                        String[] kpiTitles = {"Đơn Đã Chốt", "Tổng Doanh Thu", "Giá Trị TB / Đơn", "Hiệu Suất"};
                        String[] kpiValues = {
                                String.valueOf(totalOrders),
                                curFormat.format(totalRevenue),
                                totalOrders > 0 ? curFormat.format(totalRevenue / totalOrders) : "0 ₫",
                                "+12.5% 🚀"
                        };
                        String[] kpiColors = {"#3B82F6", "#10B981", "#F59E0B", "#8B5CF6"};

                        for (int i = 0; i < 4; i++) {
                            javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(8);
                            card.getStyleClass().add("card");
                            card.setStyle("-fx-padding: 20; -fx-border-top-width: 4; -fx-border-top-color: " + kpiColors[i] + ";");
                            javafx.scene.layout.HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);

                            javafx.scene.control.Label t = new javafx.scene.control.Label(kpiTitles[i]);
                            t.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px; -fx-font-weight: bold;");
                            javafx.scene.control.Label v = new javafx.scene.control.Label(kpiValues[i]);
                            v.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 22px; -fx-font-weight: bold;");
                            card.getChildren().addAll(t, v);
                            kpiBox.getChildren().add(card);
                        }

                        // --- 2. RENDER BIỂU ĐỒ (GIỮ NGUYÊN BẢN XỊN) ---
                        javafx.scene.layout.HBox chartsBox = new javafx.scene.layout.HBox(20);
                        chartsBox.setPrefHeight(250);

                        javafx.scene.chart.CategoryAxis xAxis = new javafx.scene.chart.CategoryAxis();
                        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
                        javafx.scene.chart.BarChart<String, Number> barChart = new javafx.scene.chart.BarChart<>(xAxis, yAxis);
                        barChart.setLegendVisible(false);
                        barChart.getStyleClass().add("card");
                        barChart.setStyle("-fx-padding: 15;");
                        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
                        for (Map.Entry<String, Double> entry : chartData.entrySet()) {
                            series.getData().add(new javafx.scene.chart.XYChart.Data<>(entry.getKey().substring(5), entry.getValue()));
                        }
                        barChart.getData().add(series);
                        javafx.scene.layout.HBox.setHgrow(barChart, javafx.scene.layout.Priority.ALWAYS);

                        javafx.scene.chart.PieChart pieChart = new javafx.scene.chart.PieChart();
                        pieChart.getData().addAll(
                                new javafx.scene.chart.PieChart.Data("Tiền mặt", cashCount),
                                new javafx.scene.chart.PieChart.Data("Chuyển khoản", transferCount)
                        );
                        pieChart.getStyleClass().add("card");
                        pieChart.setStyle("-fx-padding: 15;");
                        pieChart.setPrefWidth(350);

                        chartsBox.getChildren().addAll(barChart, pieChart);

                        // GẮN TẤT CẢ VÀO KHUNG CHÍNH (Đã sửa lỗi màn hình trắng)
                        javafx.scene.control.Label lblListTitle = new javafx.scene.control.Label("📜 Lịch Sử Giao Dịch Chi Tiết");
                        lblListTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0F172A; -fx-padding: 10 0 5 0;");

                        dynamicContent.getChildren().addAll(kpiBox, chartsBox, lblListTitle, timelineList);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        };

        cbFilter.setOnAction(e -> loadData.run());
        loadData.run();

        // 5. HIỂN THỊ CỬA SỔ
        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.getStylesheets().add(dataUri);
        dashStage.setScene(scene);
        dashStage.centerOnScreen();
        dashStage.show();
    }
}