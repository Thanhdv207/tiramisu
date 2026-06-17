package com.bakery.client.controller;

import com.bakery.shared.model.Order;
import com.bakery.shared.model.Product;
import com.bakery.shared.model.CartItem;
import com.bakery.shared.model.User;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.Socket;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AdminController implements Initializable {

    @FXML private Label lblAdminClock;
    @FXML private Button btnDashboard, btnProductManager, btnOrderHistory;

    // --- DASHBOARD ---
    @FXML private VBox viewDashboard;
    @FXML private ComboBox<String> cbDashboardFilter;
    @FXML private Label lblRevenueTitle;
    @FXML private Label lblTotalRevenue;
    @FXML private Label lblTotalOrders;
    @FXML private VBox vboxTopStaff;
    @FXML private VBox vboxTopProducts;
    @FXML private PieChart pieChartTopProducts;
    @FXML private BarChart<String, Number> barChartRevenue;

    // --- QUẢN LÝ SẢN PHẨM ---
    @FXML private VBox viewProduct;
    @FXML private javafx.scene.layout.FlowPane flowPaneProducts;
    @FXML private TextField txtId, txtName, txtCategory, txtPrice, txtStock;

    // --- LỊCH SỬ ĐƠN HÀNG ---
    @FXML private VBox viewOrderHistory;
    @FXML private ComboBox<String> cbOrderFilter;
    @FXML private TableView<Order> tableOrderHistory;
    @FXML private TableColumn<Order, String> colOrderId, colOrderDate, colOrderCustomer, colOrderCashier;
    @FXML private TableColumn<Order, Double> colOrderTotal;
    @FXML private TableColumn<Order, String> colPaymentMethod;

    @FXML private Label lblOrderDetailTitle;
    @FXML private TableView<CartItem> tableOrderDetail;
    @FXML private TableColumn<CartItem, String> colDetailName;
    @FXML private TableColumn<CartItem, Integer> colDetailQty;
    @FXML private TableColumn<CartItem, Double> colDetailPrice;

    // --- CHAT NỘI BỘ MESSENGER NỔI ---
    @FXML private VBox chatBoxContainer;
    @FXML private VBox chatMessageContainer;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextField txtChatMessage;

    // --- QUẢN LÝ NHÂN SỰ ---
    @FXML private VBox viewUser;
    @FXML private TextField txtUsername, txtFullName, txtPhone, txtSalary;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cbRole;
    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, String> colUsername, colFullName, colPhone, colRole;
    @FXML private TableColumn<User, Double> colSalary;

    private ObservableList<Product> productList = FXCollections.observableArrayList();
    private ObservableList<Order> orderList = FXCollections.observableArrayList();
    private ObservableList<CartItem> orderDetailList = FXCollections.observableArrayList();
    private ObservableList<User> userList = FXCollections.observableArrayList();

    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private Gson gson = new Gson();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initClock();

        cbDashboardFilter.getItems().addAll("Hôm nay", "Tuần này", "Tháng này", "Năm nay");
        cbDashboardFilter.setValue("Hôm nay");
        cbDashboardFilter.setOnAction(e -> loadDashboardData());

        cbOrderFilter.getItems().addAll("Hôm nay", "Tuần này", "Tháng này", "Năm nay", "Tất cả");
        cbOrderFilter.setValue("Hôm nay");
        cbOrderFilter.setOnAction(e -> loadOrderHistoryData());

        colOrderId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colOrderDate.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        colOrderCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colOrderTotal.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        colPaymentMethod.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        colOrderCashier.setCellValueFactory(new PropertyValueFactory<>("cashierName"));
        tableOrderHistory.setItems(orderList);

        colDetailName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colDetailQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colDetailPrice.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        tableOrderDetail.setItems(orderDetailList);

        tableOrderHistory.getSelectionModel().selectedItemProperty().addListener((obs, oldOrder, newOrder) -> {
            if (newOrder != null) {
                lblOrderDetailTitle.setText("Chi Tiết Đơn: " + newOrder.getOrderId());
                orderDetailList.clear();
                Map<String, Object> req = new HashMap<>();
                req.put("action", "GET_ORDER_DETAILS");
                req.put("orderId", newOrder.getOrderId());
                String res = sendToServer(req);
                if (res != null) {
                    Type listType = new TypeToken<ArrayList<CartItem>>(){}.getType();
                    List<CartItem> details = gson.fromJson(res, listType);
                    if (details != null) orderDetailList.addAll(details);
                }
            }
        });

        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colSalary.setCellValueFactory(new PropertyValueFactory<>("baseSalary"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        cbRole.setItems(FXCollections.observableArrayList("ADMIN", "NHANVIEN"));
        tableUsers.setItems(userList);

        tableUsers.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                txtUsername.setText(newSel.getUsername());
                txtFullName.setText(newSel.getFullName());
                txtPhone.setText(newSel.getPhone());
                txtSalary.setText(String.format(Locale.US, "%.0f", newSel.getBaseSalary()));
                cbRole.setValue(newSel.getRole());
                txtPassword.clear();
                txtPassword.setPromptText("Bỏ trống nếu không đổi pass");
            }
        });

        loadDashboardData();
        initChatPoller();
    }

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

    private void initClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy HH:mm:ss", new Locale("vi", "VN"));
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            lblAdminClock.setText(LocalDateTime.now().format(formatter));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    private void switchTabStyle(Button activeBtn) {
        String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: #BDC3C7; -fx-alignment: BASELINE_LEFT; -fx-padding: 10 15; -fx-cursor: hand;";
        btnDashboard.setStyle(inactiveStyle);
        btnProductManager.setStyle(inactiveStyle);
        btnOrderHistory.setStyle(inactiveStyle);
        if (activeBtn != null) {
            String activeStyle = "-fx-background-color: #34495E; -fx-text-fill: white; -fx-alignment: BASELINE_LEFT; -fx-padding: 10 15; -fx-cursor: hand;";
            activeBtn.setStyle(activeStyle);
        }
    }

    @FXML
    private void showDashboard() {
        viewProduct.setVisible(false); viewOrderHistory.setVisible(false); viewUser.setVisible(false);
        viewDashboard.setVisible(true); // <--- ĐÃ XÓA viewDashboard.toFront();
        loadDashboardData(); switchTabStyle(btnDashboard);
    }

    @FXML
    private void showProductManager() {
        viewDashboard.setVisible(false); viewOrderHistory.setVisible(false); viewUser.setVisible(false);
        viewProduct.setVisible(true); // <--- ĐÃ XÓA viewProduct.toFront();
        loadProductData(); switchTabStyle(btnProductManager);
    }

    @FXML
    private void showOrderHistory() {
        viewDashboard.setVisible(false); viewProduct.setVisible(false); viewUser.setVisible(false);
        viewOrderHistory.setVisible(true); // <--- ĐÃ XÓA viewOrderHistory.toFront();
        loadOrderHistoryData(); switchTabStyle(btnOrderHistory);
        lblOrderDetailTitle.setText("Chi Tiết Đơn Hàng: (Chưa chọn)");
        orderDetailList.clear();
    }

    @FXML
    private void showUserManager() {
        viewDashboard.setVisible(false); viewProduct.setVisible(false); viewOrderHistory.setVisible(false);
        viewUser.setVisible(true); // <--- ĐÃ XÓA viewUser.toFront();
        loadUserDataFromServer(); switchTabStyle(null);
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadDashboardData();
    }

    private void loadDashboardData() {
        String filter = cbDashboardFilter.getValue();
        lblRevenueTitle.setText("DOANH THU (" + filter.toUpperCase() + ")");
        pieChartTopProducts.getData().clear();
        barChartRevenue.getData().clear();

        // 1. DOANH THU TỔNG
        Map<String, Object> reqRev = new HashMap<>(); reqRev.put("action", "GET_REVENUE"); reqRev.put("filter", filter);
        String resRev = sendToServer(reqRev);
        if (resRev != null && !resRev.trim().isEmpty()) {
            try {
                lblTotalRevenue.setText(currencyFormat.format(Double.parseDouble(resRev.replace("\"", ""))));
            } catch (NumberFormatException e) {
                lblTotalRevenue.setText("0 đ");
            }
        }

        // 2. BIỂU ĐỒ CỘT
        Map<String, Object> reqChart = new HashMap<>(); reqChart.put("action", "GET_CHART_DATA"); reqChart.put("filter", filter);
        String resChart = sendToServer(reqChart);
        if (resChart != null) {
            try {
                Map<String, Double> chartData = gson.fromJson(resChart, new TypeToken<Map<String, Double>>(){}.getType());
                if (chartData != null) {
                    XYChart.Series<String, Number> series = new XYChart.Series<>(); series.setName("Biểu đồ doanh thu");
                    for (Map.Entry<String, Double> entry : chartData.entrySet()) {
                        series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
                    }
                    barChartRevenue.getData().add(series);
                }
            } catch (Exception e) {}
        }

        // 3. VẼ TOP 5 SẢN PHẨM TỪ API BIỂU ĐỒ TRÒN

        if (vboxTopProducts != null) vboxTopProducts.getChildren().clear();
        Map<String, Object> reqTop = new HashMap<>(); reqTop.put("action", "GET_TOP_PRODUCTS"); reqTop.put("filter", filter);
        String resTop = sendToServer(reqTop);
        if (resTop != null) {
            try {
                Map<String, Integer> topProducts = gson.fromJson(resTop, new TypeToken<Map<String, Integer>>(){}.getType());
                if (topProducts != null && !topProducts.isEmpty()) {
                    ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

                    // Sắp xếp mảng giảm dần để lấy Top
                    List<Map.Entry<String, Integer>> sortedProd = new ArrayList<>(topProducts.entrySet());
                    sortedProd.sort((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()));

                    int rank = 1;
                    for (Map.Entry<String, Integer> entry : sortedProd) {
                        pieData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));

                        // Tiện tay nhét luôn vào VBox Top Sản Phẩm bên dưới
                        if (vboxTopProducts != null && rank <= 5) {
                            String text = String.format("%d. %s - Đã bán: %d cái", rank++, entry.getKey(), entry.getValue());
                            Label lbl = new Label(text);
                            lbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #334155;");
                            vboxTopProducts.getChildren().add(lbl);
                        }
                    }
                    pieChartTopProducts.setData(pieData);
                } else if (vboxTopProducts != null) {
                    vboxTopProducts.getChildren().add(new Label("Chưa có dữ liệu."));
                }
            } catch (Exception e) {}
        }


        // 4.ĐẾM SỐ ĐƠN & TOP NHÂN VIÊN TỪ API DANH SÁCH ĐƠN
        if (vboxTopStaff != null) vboxTopStaff.getChildren().clear();
        Map<String, Object> reqOrders = new HashMap<>(); reqOrders.put("action", "GET_ORDERS"); reqOrders.put("filter", filter);
        String resOrders = sendToServer(reqOrders);
        if (resOrders != null && resOrders.startsWith("[")) {
            try {
                com.google.gson.JsonArray ordersArr = com.google.gson.JsonParser.parseString(resOrders).getAsJsonArray();

                // Lấy luôn số lượng phần tử của mảng đắp vào "TỔNG SỐ ĐƠN CHỐT"
                if (lblTotalOrders != null) lblTotalOrders.setText(String.valueOf(ordersArr.size()));

                // Tự tay gom nhóm và tính toán doanh thu cho từng thu ngân
                Map<String, double[]> staffStats = new HashMap<>();
                for (int i = 0; i < ordersArr.size(); i++) {
                    com.google.gson.JsonObject o = ordersArr.get(i).getAsJsonObject();
                    String staff = o.has("cashierName") && !o.get("cashierName").isJsonNull() ? o.get("cashierName").getAsString() : "Hệ thống";
                    double total = o.has("totalAmount") ? o.get("totalAmount").getAsDouble() : 0.0;

                    if (!staffStats.containsKey(staff)) {
                        staffStats.put(staff, new double[]{0.0, 0.0});
                    }
                    staffStats.get(staff)[0] += 1; // [0] là Số đơn
                    staffStats.get(staff)[1] += total; // [1] là Doanh thu
                }

                // Sắp xếp nhân viên giỏi nhất lên trên
                List<Map.Entry<String, double[]>> sortedStaff = new ArrayList<>(staffStats.entrySet());
                sortedStaff.sort((e1, e2) -> Double.compare(e2.getValue()[1], e1.getValue()[1]));

                int rank = 1;
                for (Map.Entry<String, double[]> entry : sortedStaff) {
                    if (rank > 5) break;
                    String text = String.format("%d. %s - %s (%d đơn)",
                            rank++, entry.getKey(),
                            currencyFormat.format(entry.getValue()[1]),
                            (int) entry.getValue()[0]);
                    Label lbl = new Label(text);
                    lbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #334155;");
                    vboxTopStaff.getChildren().add(lbl);
                }

                if (sortedStaff.isEmpty() && vboxTopStaff != null) {
                    vboxTopStaff.getChildren().add(new Label("Chưa có dữ liệu."));
                }
            } catch (Exception e) {}
        }
    }
    private void loadProductData() {
        flowPaneProducts.getChildren().clear();
        Map<String, Object> req = new HashMap<>(); req.put("action", "GET_PRODUCTS");
        String res = sendToServer(req);
        if (res != null) {
            try {
                List<Product> products = gson.fromJson(res, new TypeToken<ArrayList<Product>>(){}.getType());
                if (products != null) {
                    for (Product p : products) { flowPaneProducts.getChildren().add(createEnhancedCard(p)); }
                }
            } catch (Exception e) {}
        }
    }

    private javafx.scene.layout.VBox createEnhancedCard(Product p) {
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-pref-width: 210; -fx-alignment: TOP_CENTER;");
        String defaultImgUrl = "https://cdn-icons-png.flaticon.com/512/992/992747.png";
        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(new javafx.scene.image.Image(defaultImgUrl, true));
        imageView.setFitWidth(100); imageView.setFitHeight(100); imageView.setPreserveRatio(true);

        Label name = new Label(p.getProductName().toUpperCase());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2C3E50;"); name.setWrapText(true); name.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        Label price = new Label(currencyFormat.format(p.getPrice())); price.setStyle("-fx-font-size: 15px; -fx-text-fill: #E67E22; -fx-font-weight: bold;");
        Label stockBadge = new Label("KHO: " + p.getStockQuantity());
        String stockColor = (p.getStockQuantity() < 10) ? "#E74C3C" : "#27AE60";
        stockBadge.setStyle("-fx-background-color: " + stockColor + "22; -fx-text-fill: " + stockColor + "; -fx-padding: 3 8; -fx-background-radius: 5; -fx-font-size: 12px; -fx-font-weight: bold;");

        Button btnEdit = new Button("SỬA"); btnEdit.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 15; -fx-cursor: hand;"); btnEdit.setPrefWidth(60);
        btnEdit.setOnAction(e -> { txtId.setText(p.getProductId()); txtId.setDisable(true); txtName.setText(p.getProductName()); txtCategory.setText(String.valueOf(p.getCategoryId())); txtPrice.setText(String.format(Locale.US, "%.0f", p.getPrice())); txtStock.setText(String.valueOf(p.getStockQuantity())); });
        Button btnDelete = new Button("XÓA"); btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #E74C3C; -fx-border-color: #E74C3C; -fx-border-radius: 15; -fx-font-size: 11px; -fx-cursor: hand;"); btnDelete.setPrefWidth(60);
        btnDelete.setOnAction(e -> { txtId.setText(p.getProductId()); handleDeleteProduct(); });

        javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(10, btnEdit, btnDelete); actions.setAlignment(javafx.geometry.Pos.CENTER); actions.setStyle("-fx-padding: 10 0 0 0;");
        card.getChildren().addAll(imageView, name, price, stockBadge, actions);
        return card;
    }

    private void loadOrderHistoryData() {
        orderList.clear();
        Map<String, Object> req = new HashMap<>(); req.put("action", "GET_ORDERS"); req.put("filter", cbOrderFilter.getValue());
        String res = sendToServer(req);
        if (res != null) {
            try {
                List<Order> orders = gson.fromJson(res, new TypeToken<ArrayList<Order>>(){}.getType());
                if (orders != null) orderList.addAll(orders);
            } catch (Exception e) {}
        }
    }

    @FXML
    private void handleAddProduct() {
        try {
            Product p = new Product(txtId.getText(), txtName.getText(), Integer.parseInt(txtCategory.getText()), Double.parseDouble(txtPrice.getText()), Integer.parseInt(txtStock.getText()));
            Map<String, Object> req = new HashMap<>(); req.put("action", "ADD_PRODUCT"); req.put("product", p);
            String res = sendToServer(req);
            Map<String, String> response = gson.fromJson(res, new TypeToken<Map<String, String>>(){}.getType());
            if (response != null && "SUCCESS".equals(response.get("status"))) { showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã thêm sản phẩm!"); loadProductData(); clearForm(); }
        } catch (Exception e) { showAlert(Alert.AlertType.WARNING, "Lỗi", "Kiểm tra lại dữ liệu nhập!"); }
    }

    @FXML
    private void handleUpdateProduct() {
        try {
            Product p = new Product(txtId.getText(), txtName.getText(), Integer.parseInt(txtCategory.getText()), Double.parseDouble(txtPrice.getText()), Integer.parseInt(txtStock.getText()));
            Map<String, Object> req = new HashMap<>(); req.put("action", "UPDATE_PRODUCT"); req.put("product", p);
            String res = sendToServer(req);
            Map<String, String> response = gson.fromJson(res, new TypeToken<Map<String, String>>(){}.getType());
            if (response != null && "SUCCESS".equals(response.get("status"))) { showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật!"); loadProductData(); clearForm(); }
        } catch (Exception e) { showAlert(Alert.AlertType.WARNING, "Lỗi", "Kiểm tra lại dữ liệu nhập!"); }
    }

    @FXML
    private void handleDeleteProduct() {
        if (txtId.getText().isEmpty()) return;
        Map<String, Object> req = new HashMap<>(); req.put("action", "DELETE_PRODUCT"); req.put("productId", txtId.getText());
        String res = sendToServer(req);
        Map<String, String> response = gson.fromJson(res, new TypeToken<Map<String, String>>(){}.getType());
        if (response != null && "SUCCESS".equals(response.get("status"))) { showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã xóa!"); loadProductData(); clearForm(); }
    }

    private void loadUserDataFromServer() {
        userList.clear();
        Map<String, Object> req = new HashMap<>();
        req.put("action", "GET_ALL_USERS");

        String res = sendToServer(req);
        if (res != null) {
            try {
                List<User> list = gson.fromJson(res, new TypeToken<List<User>>(){}.getType());
                if (list != null) {
                    userList.setAll(list);
                }
            } catch (Exception e) {
                System.out.println("[-] Lỗi dịch dữ liệu Nhân viên: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleAddUser() {
        if (txtUsername.getText().isEmpty()) { showAlert(Alert.AlertType.WARNING, "Lỗi", "Nhập tên đăng nhập!"); return; }
        try {
            User u = new User(txtUsername.getText(), txtPassword.getText(), cbRole.getValue(), txtFullName.getText(), txtPhone.getText(), Double.parseDouble(txtSalary.getText()));
            Map<String, Object> req = new HashMap<>(); req.put("action", "ADD_USER"); req.put("user", u);

            String res = sendToServer(req);
            Map<String, String> response = gson.fromJson(res, new TypeToken<Map<String, String>>(){}.getType());

            if (response != null && "SUCCESS".equals(response.get("status"))) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã thêm nhân viên!"); loadUserDataFromServer(); clearUserForm();
            } else { showAlert(Alert.AlertType.ERROR, "Thông báo", "Tài khoản đã tồn tại hoặc lỗi!"); }
        } catch (Exception e) { showAlert(Alert.AlertType.WARNING, "Lỗi", "Kiểm tra lại dữ liệu lương!"); }
    }

    @FXML
    private void handleUpdateUser() {
        try {
            User u = new User(txtUsername.getText(), txtPassword.getText(), cbRole.getValue(), txtFullName.getText(), txtPhone.getText(), Double.parseDouble(txtSalary.getText()));
            Map<String, Object> req = new HashMap<>(); req.put("action", "UPDATE_USER"); req.put("user", u);

            String res = sendToServer(req);
            Map<String, String> response = gson.fromJson(res, new TypeToken<Map<String, String>>(){}.getType());

            if (response != null && "SUCCESS".equals(response.get("status"))) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật!"); loadUserDataFromServer(); clearUserForm();
            } else { showAlert(Alert.AlertType.ERROR, "Lỗi", "Cập nhật thất bại!"); }
        } catch (Exception e) { showAlert(Alert.AlertType.WARNING, "Lỗi", "Kiểm tra lại dữ liệu lương!"); }
    }

    @FXML
    private void handleDeleteUser() {
        if (txtUsername.getText().isEmpty()) return;
        Map<String, Object> req = new HashMap<>(); req.put("action", "DELETE_USER"); req.put("username", txtUsername.getText());

        String res = sendToServer(req);
        Map<String, String> response = gson.fromJson(res, new TypeToken<Map<String, String>>(){}.getType());

        if (response != null && "SUCCESS".equals(response.get("status"))) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã xóa!"); loadUserDataFromServer(); clearUserForm();
        } else { showAlert(Alert.AlertType.ERROR, "Lỗi", "Xóa thất bại!"); }
    }

    @FXML
    private void handleShiftReport() {
        Map<String, Object> req = new HashMap<>();
        req.put("action", "GET_SHIFT_REPORT");

        String res = sendToServer(req);
        if (res != null) {
            try {
                Map<String, String> response = gson.fromJson(res, new TypeToken<Map<String, String>>(){}.getType());
                if ("SUCCESS".equals(response.get("status"))) {

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Báo Cáo Doanh Thu Cuối Ngày");
                    alert.setHeaderText("📊 THỐNG KÊ CA LÀM HÔM NAY");

                    VBox vbox = new VBox(15);
                    vbox.setStyle("-fx-background-color: transparent; -fx-padding: 10;");

                    String rawData = response.get("data");

                    if (rawData == null || rawData.trim().isEmpty() || rawData.contains("chưa có nhân viên nào")) {
                        Label emptyLbl = new Label("Trắng tay! Hôm nay chưa có đơn nào chốt được cả 😭");
                        emptyLbl.setStyle("-fx-font-size: 16px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        vbox.getChildren().add(emptyLbl);
                    } else {
                        String[] reports = rawData.split("\n\n");
                        for (String rep : reports) {
                            if (rep.trim().isEmpty()) continue;

                            Label lbl = new Label(rep.trim());
                            lbl.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; " +
                                    "-fx-font-size: 15px; -fx-text-fill: #2c3e50; " +
                                    "-fx-background-color: #F8F9FA; -fx-padding: 15 20; " +
                                    "-fx-background-radius: 10; -fx-border-color: #BDC3C7; " +
                                    "-fx-border-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
                            vbox.getChildren().add(lbl);
                        }
                    }

                    alert.getDialogPane().setContent(vbox);
                    alert.setGraphic(null);

                    Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
                    if (okButton != null) {
                        okButton.setStyle("-fx-background-color: #8E44AD; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 25; -fx-background-radius: 5;");
                    }

                    alert.showAndWait();
                }
            } catch (Exception e) {
                System.out.println("[-] Lỗi dịch báo cáo ca làm: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleToggleChat() {
        if (chatBoxContainer != null) {
            boolean isVisible = chatBoxContainer.isVisible();
            chatBoxContainer.setVisible(!isVisible);
            chatBoxContainer.setManaged(!isVisible);
            if (!isVisible) {
                chatBoxContainer.toFront();
            }
        }
    }

    @FXML
    private void handleSendChat() {
        String msg = txtChatMessage.getText().trim();
        if (msg.isEmpty()) return;
        Map<String, Object> req = new HashMap<>();
        req.put("action", "SEND_CHAT");
        req.put("sender", "ADMIN");
        req.put("message", msg);
        sendToServer(req);
        txtChatMessage.clear();
        loadChatData();
    }

    private void loadChatData() {
        Map<String, Object> req = new HashMap<>(); req.put("action", "GET_CHAT");
        String res = sendToServer(req);
        if (res != null && res.trim().startsWith("[")) {
            try {
                List<String> msgs = gson.fromJson(res, new TypeToken<List<String>>(){}.getType());
                if (msgs != null) {
                    if (chatMessageContainer.getChildren().size() != msgs.size()) {
                        chatMessageContainer.getChildren().clear();

                        for (String msgStr : msgs) {
                            String sender = "Hệ thống";
                            String text = msgStr;

                            if (msgStr.contains(": ")) {
                                sender = msgStr.substring(0, msgStr.indexOf(":"));
                                text = msgStr.substring(msgStr.indexOf(":") + 2);
                            }

                            boolean isMe = sender.equalsIgnoreCase("ADMIN");

                            Label bubble = new Label(text);
                            bubble.setWrapText(true);
                            bubble.setMaxWidth(220);

                            javafx.scene.layout.HBox row = new javafx.scene.layout.HBox();
                            row.setPadding(new javafx.geometry.Insets(2, 0, 2, 0));

                            if (isMe) {
                                bubble.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 15 15 0 15; -fx-font-size: 13px;");
                                row.getChildren().add(bubble);
                                row.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                            } else {
                                javafx.scene.layout.VBox otherBox = new javafx.scene.layout.VBox(2);
                                Label senderLbl = new Label(sender);
                                senderLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8; -fx-font-weight: bold;");
                                bubble.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: #1E293B; -fx-padding: 8 12; -fx-background-radius: 15 15 15 0; -fx-font-size: 13px;");

                                otherBox.getChildren().addAll(senderLbl, bubble);
                                row.getChildren().add(otherBox);
                                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                            }
                            chatMessageContainer.getChildren().add(row);
                        }
                        javafx.application.Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
                    }
                }
            } catch (Exception e) {}
        }
    }


    private void initChatPoller() {
        Timeline chatTimer = new Timeline(new KeyFrame(Duration.seconds(2), e -> loadChatData()));
        chatTimer.setCycleCount(Timeline.INDEFINITE); chatTimer.play();
    }

    @FXML
    private void clearForm() {
        txtId.clear(); txtId.setDisable(false); txtName.clear(); txtCategory.clear(); txtPrice.clear(); txtStock.clear();
    }

    private void clearUserForm() {
        txtUsername.clear(); txtUsername.setDisable(false); txtPassword.clear(); txtFullName.clear(); txtPhone.clear(); txtSalary.clear(); cbRole.setValue(null);
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/bakery/view/LoginView.fxml"));
            javafx.stage.Stage stage = (javafx.stage.Stage) lblTotalRevenue.getScene().getWindow();
            stage.setMaximized(false); stage.setResizable(false);
            stage.setScene(new javafx.scene.Scene(loader.load(), 800, 500));
            stage.setTitle("Bakery Manager - Đăng Nhập"); stage.centerOnScreen(); stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- HÀM XEM DASHBOARD NĂNG SUẤT (BẢN JAVAFX SAAS CHUẨN) ---
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

        javafx.stage.Stage dashStage = new javafx.stage.Stage();
        dashStage.setTitle("Productivity Dashboard - " + selectedUser.getFullName());
        dashStage.setWidth(1000);
        dashStage.setHeight(750);

        String css = ".root-pane { -fx-background-color: #F8FAFC; -fx-font-family: 'Segoe UI', Inter, sans-serif; }"
                + ".card { -fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(148, 163, 184, 0.15), 10, 0, 0, 4); }"
                + ".card:hover { -fx-effect: dropshadow(three-pass-box, rgba(37, 99, 235, 0.12), 15, 0, 0, 8); -fx-translate-y: -2; }"
                + ".combo-box { -fx-background-color: white; -fx-border-color: #CBD5E1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-weight: bold; -fx-text-fill: #1E293B; }"
                + ".combo-box .list-cell { -fx-padding: 8; }"
                + ".badge-active { -fx-background-color: #D1FAE5; -fx-text-fill: #10B981; -fx-background-radius: 20; -fx-padding: 4 12; -fx-font-weight: bold; -fx-font-size: 11px; }"
                + ".badge-cash { -fx-background-color: #DBEAFE; -fx-text-fill: #2563EB; -fx-background-radius: 6; -fx-padding: 4 8; -fx-font-size: 11px; -fx-font-weight: bold; }"
                + ".badge-transfer { -fx-background-color: #FEF3C7; -fx-text-fill: #D97706; -fx-background-radius: 6; -padding: 4 8; -fx-font-size: 11px; -fx-font-weight: bold; }"
                + ".scroll-pane { -fx-background-color: transparent; } .scroll-pane > .viewport { -fx-background-color: transparent; }";

        String dataUri = "data:text/css;charset=utf-8," + css.replace(" ", "%20").replace("\n", "");

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(20);
        root.getStyleClass().add("root-pane");
        root.setStyle("-fx-padding: 25;");

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

        javafx.scene.layout.VBox dynamicContent = new javafx.scene.layout.VBox(20);
        javafx.scene.layout.VBox.setVgrow(dynamicContent, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.control.ScrollPane mainScroll = new javafx.scene.control.ScrollPane(dynamicContent);
        mainScroll.setFitToWidth(true);
        mainScroll.getStyleClass().add("scroll-pane");
        javafx.scene.layout.VBox.setVgrow(mainScroll, javafx.scene.layout.Priority.ALWAYS);

        root.getChildren().addAll(header, mainScroll);

        java.text.NumberFormat curFormat = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("vi", "VN"));

        Runnable loadData = () -> {
            dynamicContent.getChildren().clear();
            java.util.Map<String, Object> req = new java.util.HashMap<>();
            req.put("action", "GET_ORDERS_BY_USER");
            req.put("username", selectedUser.getUsername());
            req.put("filter", cbFilter.getValue());

            String res = sendToServer(req);
            if (res != null) {
                try {
                    java.util.Map<String, String> response = gson.fromJson(res, new com.google.gson.reflect.TypeToken<java.util.Map<String, String>>(){}.getType());
                    if ("SUCCESS".equals(response.get("status"))) {

                        String rawData = response.get("data");
                        com.google.gson.JsonObject jsonRoot = com.google.gson.JsonParser.parseString(rawData).getAsJsonObject();

                        if (jsonRoot.has("error")) {
                            javafx.scene.control.Label lblErr = new javafx.scene.control.Label("Lỗi Server: " + jsonRoot.get("error").getAsString());
                            lblErr.setStyle("-fx-text-fill: red; -fx-padding: 20;");
                            dynamicContent.getChildren().add(lblErr);
                            return;
                        }

                        int totalOrders = jsonRoot.get("totalOrders").getAsInt();
                        if (totalOrders == 0) {
                            javafx.scene.control.Label lblEmpty = new javafx.scene.control.Label("📭 Không có dữ liệu giao dịch trong khoảng thời gian này.");
                            lblEmpty.setStyle("-fx-font-size: 16px; -fx-text-fill: #94A3B8; -fx-padding: 50; -fx-alignment: center;");
                            lblEmpty.setMaxWidth(Double.MAX_VALUE);
                            dynamicContent.getChildren().add(lblEmpty);
                            return;
                        }

                        double totalRevenue = jsonRoot.get("totalRevenue").getAsDouble();
                        int cashCount = jsonRoot.get("cashCount").getAsInt();
                        int transferCount = jsonRoot.get("transferCount").getAsInt();

                        javafx.scene.layout.HBox kpiBox = new javafx.scene.layout.HBox(15);
                        String[] kpiTitles = {"Đơn Đã Chốt", "Tổng Doanh Thu", "Giá Trị TB / Đơn", "Hiệu Suất"};
                        String[] kpiValues = {
                                String.valueOf(totalOrders),
                                curFormat.format(totalRevenue),
                                curFormat.format(totalRevenue / totalOrders),
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

                        javafx.scene.layout.HBox chartsBox = new javafx.scene.layout.HBox(20);
                        chartsBox.setPrefHeight(250);

                        javafx.scene.chart.CategoryAxis xAxis = new javafx.scene.chart.CategoryAxis();
                        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
                        javafx.scene.chart.BarChart<String, Number> barChart = new javafx.scene.chart.BarChart<>(xAxis, yAxis);
                        barChart.setLegendVisible(false);
                        barChart.getStyleClass().add("card");
                        barChart.setStyle("-fx-padding: 15;");

                        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
                        com.google.gson.JsonObject chartObj = jsonRoot.getAsJsonObject("chartData");

                        for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : chartObj.entrySet()) {
                            String shortDate = entry.getKey().substring(5);
                            series.getData().add(new javafx.scene.chart.XYChart.Data<>(shortDate, entry.getValue().getAsDouble()));
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

                        javafx.scene.layout.VBox timelineList = new javafx.scene.layout.VBox(15);
                        com.google.gson.JsonArray historyArray = jsonRoot.getAsJsonArray("history");
                        String currentDayTracker = "";

                        for (int i = 0; i < historyArray.size(); i++) {
                            com.google.gson.JsonObject orderObj = historyArray.get(i).getAsJsonObject();

                            String oDate = orderObj.get("date").getAsString();
                            String oTime = orderObj.get("time").getAsString();
                            String oId = orderObj.get("id").getAsString();
                            String oCustomer = orderObj.get("customer").getAsString();
                            double oAmt = orderObj.get("amount").getAsDouble();
                            boolean isCash = orderObj.get("isCash").getAsBoolean();
                            String oItems = orderObj.get("items").getAsString();

                            if (!oDate.equals(currentDayTracker)) {
                                currentDayTracker = oDate;
                                javafx.scene.control.Label lblDay = new javafx.scene.control.Label("📅 " + oDate);
                                lblDay.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #64748B; -fx-padding: 15 0 5 0;");
                                timelineList.getChildren().add(lblDay);
                            }

                            javafx.scene.layout.VBox orderCard = new javafx.scene.layout.VBox(12);
                            orderCard.getStyleClass().add("card");
                            orderCard.setStyle("-fx-padding: 18;");

                            javafx.scene.layout.HBox cardHeader = new javafx.scene.layout.HBox(15);
                            cardHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                            javafx.scene.control.Label lblIcon = new javafx.scene.control.Label("🛍️");
                            lblIcon.setStyle("-fx-font-size: 20px; -fx-background-color: #F1F5F9; -fx-background-radius: 8; -fx-padding: 8;");

                            javafx.scene.layout.VBox orderInfo = new javafx.scene.layout.VBox(2);
                            javafx.scene.control.Label lblId = new javafx.scene.control.Label(oId);
                            lblId.setStyle("-fx-font-weight: bold; -fx-text-fill: #2563EB; -fx-font-size: 14px;");
                            javafx.scene.control.Label lblTimeCus = new javafx.scene.control.Label("🕒 " + oTime + " • 👤 " + oCustomer);
                            lblTimeCus.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
                            orderInfo.getChildren().addAll(lblId, lblTimeCus);

                            javafx.scene.layout.Region oSpacer = new javafx.scene.layout.Region();
                            javafx.scene.layout.HBox.setHgrow(oSpacer, javafx.scene.layout.Priority.ALWAYS);

                            javafx.scene.layout.VBox moneyBox = new javafx.scene.layout.VBox(5);
                            moneyBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                            javafx.scene.control.Label lblAmt = new javafx.scene.control.Label(curFormat.format(oAmt));
                            lblAmt.setStyle("-fx-font-weight: bold; -fx-text-fill: #10B981; -fx-font-size: 15px;");
                            javafx.scene.control.Label lblPayType = new javafx.scene.control.Label(isCash ? "Tiền mặt" : "Chuyển khoản");
                            lblPayType.getStyleClass().add(isCash ? "badge-cash" : "badge-transfer");
                            moneyBox.getChildren().addAll(lblAmt, lblPayType);

                            cardHeader.getChildren().addAll(lblIcon, orderInfo, oSpacer, moneyBox);

                            javafx.scene.control.Label lblItemsText = new javafx.scene.control.Label(oItems);
                            lblItemsText.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px; -fx-text-fill: #334155; -fx-background-color: #F8FAFC; -fx-padding: 12; -fx-background-radius: 6; -fx-border-color: #E2E8F0; -fx-border-radius: 6;");
                            lblItemsText.setMaxWidth(Double.MAX_VALUE);

                            orderCard.getChildren().addAll(cardHeader, lblItemsText);
                            timelineList.getChildren().add(orderCard);
                        }

                        javafx.scene.control.Label lblListTitle = new javafx.scene.control.Label("📜 Lịch Sử Giao Dịch Chi Tiết");
                        lblListTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0F172A; -fx-padding: 10 0 5 0;");

                        javafx.scene.layout.HBox analyticsBox = new javafx.scene.layout.HBox(20);

                        javafx.scene.layout.VBox topProductCard = new javafx.scene.layout.VBox(10);
                        topProductCard.getStyleClass().add("card");
                        topProductCard.setStyle("-fx-padding: 15;");
                        javafx.scene.layout.HBox.setHgrow(topProductCard, javafx.scene.layout.Priority.ALWAYS);

                        javafx.scene.control.Label lblTopP = new javafx.scene.control.Label("🏆 Top Sản Phẩm Bán Chạy");
                        lblTopP.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #0F172A;");
                        topProductCard.getChildren().add(lblTopP);

                        com.google.gson.JsonArray topPArr = jsonRoot.getAsJsonArray("topProducts");
                        if (topPArr != null && topPArr.size() > 0) {
                            for (int i = 0; i < topPArr.size(); i++) {
                                com.google.gson.JsonObject p = topPArr.get(i).getAsJsonObject();
                                javafx.scene.control.Label lblItem = new javafx.scene.control.Label(
                                        (i+1) + ". " + p.get("name").getAsString() + " (" + p.get("qty").getAsInt() + " cái)"
                                );
                                lblItem.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");
                                topProductCard.getChildren().add(lblItem);
                            }
                        } else {
                            javafx.scene.control.Label lblItem = new javafx.scene.control.Label("Dữ liệu đang cập nhật...");
                            lblItem.setStyle("-fx-font-size: 13px; -fx-text-fill: #94A3B8; -fx-font-style: italic;");
                            topProductCard.getChildren().add(lblItem);
                        }

                        javafx.scene.layout.VBox topHourCard = new javafx.scene.layout.VBox(10);
                        topHourCard.getStyleClass().add("card");
                        topHourCard.setStyle("-fx-padding: 15;");
                        javafx.scene.layout.HBox.setHgrow(topHourCard, javafx.scene.layout.Priority.ALWAYS);

                        javafx.scene.control.Label lblTopH = new javafx.scene.control.Label("⏰ Khung Giờ Vàng (Nhiều Đơn Nhất)");
                        lblTopH.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #0F172A;");
                        topHourCard.getChildren().add(lblTopH);

                        com.google.gson.JsonArray topHArr = jsonRoot.getAsJsonArray("topHours");
                        if (topHArr != null && topHArr.size() > 0) {
                            for (int i = 0; i < topHArr.size(); i++) {
                                com.google.gson.JsonObject h = topHArr.get(i).getAsJsonObject();
                                javafx.scene.control.Label lblItem = new javafx.scene.control.Label(
                                        "🔥 " + h.get("hour").getAsString() + " (Đã chốt " + h.get("count").getAsInt() + " đơn)"
                                );
                                lblItem.setStyle("-fx-font-size: 13px; -fx-text-fill: #D97706; -fx-font-weight: bold;");
                                topHourCard.getChildren().add(lblItem);
                            }
                        } else {
                            javafx.scene.control.Label lblItem = new javafx.scene.control.Label("Dữ liệu đang cập nhật...");
                            lblItem.setStyle("-fx-font-size: 13px; -fx-text-fill: #94A3B8; -fx-font-style: italic;");
                            topHourCard.getChildren().add(lblItem);
                        }

                        analyticsBox.getChildren().addAll(topProductCard, topHourCard);
                        dynamicContent.getChildren().addAll(kpiBox, chartsBox, analyticsBox, lblListTitle, timelineList);

                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        };

        cbFilter.setOnAction(e -> loadData.run());
        loadData.run();

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.getStylesheets().add(dataUri);
        dashStage.setScene(scene);
        dashStage.centerOnScreen();
        dashStage.show();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}