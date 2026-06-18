package com.bakery.client.controller;

import com.bakery.shared.model.CartItem;
import com.bakery.shared.model.Product;
import com.bakery.client.utils.PDFExportUtil;
import com.bakery.client.utils.QRCodeUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
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

public class POSController implements Initializable {

    // --- BIẾN GIAO DIỆN ---
    @FXML private FlowPane productContainer;
    @FXML private TableView<CartItem> tableCart;
    @FXML private TableColumn<CartItem, String> colName;
    @FXML private TableColumn<CartItem, Integer> colQty;
    @FXML private TableColumn<CartItem, Double> colTotal;
    @FXML private Label lblTotalAmount;
    @FXML private Label lblClock;
    @FXML private TextField txtCustomerName;
    @FXML private TextField txtCustomerPhone;
    @FXML private TextField txtSearchProduct;
    @FXML private ComboBox<String> cbPaymentMethod;


    // --- CHAT NỘI BỘ MESSENGER NỔI ---
    @FXML private VBox chatBoxContainer; // <-- Đổi thành Container
    @FXML private VBox chatMessageContainer;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextField txtChatMessage;

    // --- CÔNG CỤ XỬ LÝ ---
    private ObservableList<CartItem> cartList = FXCollections.observableArrayList();
    private Locale vnLocale = new Locale("vi", "VN");
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(vnLocale);
    private Gson gson = new Gson();
    private Map<String, Product> productMap = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        colName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        tableCart.setItems(cartList);

        cbPaymentMethod.getItems().addAll("Tiền mặt", "Chuyển khoản");
        cbPaymentMethod.setValue("Chuyển khoản");

        initClock();
        loadProductsFromDatabase();
        initChatPoller();

        txtSearchProduct.textProperty().addListener((observable, oldValue, newValue) -> filterProducts(newValue));
    }

    private String sendToServer(Map<String, Object> requestData) {
        try (Socket socket = new Socket("localhost", 8888);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))) {
            out.println(gson.toJson(requestData));
            return in.readLine();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void loadProductsFromDatabase() {
        productContainer.getChildren().clear();
        productMap.clear();

        Map<String, Object> request = new HashMap<>();
        request.put("action", "GET_PRODUCTS");

        String responseJson = sendToServer(request);
        if (responseJson != null) {
            Type listType = new TypeToken<ArrayList<Product>>(){}.getType();
            List<Product> products = gson.fromJson(responseJson, listType);

            if (products != null) {
                for (Product p : products) {
                    productMap.put(p.getProductId(), p);
                    createProductCard(p);
                }
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể lấy dữ liệu sản phẩm!");
        }
    }

    private void filterProducts(String keyword) {
        productContainer.getChildren().clear();
        if (keyword == null || keyword.trim().isEmpty()) {
            for (Product p : productMap.values()) createProductCard(p);
            return;
        }
        String lowerCaseKeyword = keyword.toLowerCase();
        for (Product p : productMap.values()) {
            if (p.getProductName().toLowerCase().contains(lowerCaseKeyword)) {
                createProductCard(p);
            }
        }
    }

    private void createProductCard(Product p) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(120); imageView.setFitHeight(100); imageView.setPreserveRatio(true);

        try {
            URL imageUrl = getClass().getResource("/images/" + p.getProductId() + ".png");
            if (imageUrl == null) imageUrl = getClass().getResource("/images/" + p.getProductId() + ".jpg");
            if (imageUrl != null) imageView.setImage(new Image(imageUrl.toExternalForm()));
            else {
                URL defaultUrl = getClass().getResource("/images/default.png");
                if (defaultUrl != null) imageView.setImage(new Image(defaultUrl.toExternalForm()));
            }
        } catch (Exception e) { e.printStackTrace(); }

        Label lblName = new Label(p.getProductName());
        lblName.setStyle("-fx-font-weight: bold; -fx-text-fill: #2C3E50; -fx-font-size: 13px;");
        Label lblPrice = new Label(currencyFormat.format(p.getPrice()));
        lblPrice.setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold; -fx-font-size: 14px;");
        Label lblStock = new Label("(Kho: " + p.getStockQuantity() + ")");
        lblStock.setStyle("-fx-font-size: 11px; -fx-text-fill: #7F8C8D;");

        VBox vbox = new VBox(5, imageView, lblName, lblPrice, lblStock);
        vbox.setAlignment(Pos.CENTER); vbox.setPadding(new javafx.geometry.Insets(10));

        Button btn = new Button(); btn.setGraphic(vbox); btn.setPrefSize(160, 180); btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        if (p.getStockQuantity() <= 0) {
            btn.setDisable(true);
            btn.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 10; -fx-border-color: #BDC3C7; -fx-border-radius: 10; -fx-opacity: 0.6;");
            lblStock.setText("(Hết hàng)");
            lblStock.setStyle("-fx-font-size: 11px; -fx-text-fill: #E74C3C; -fx-font-weight: bold;");
        } else {
            btn.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #E0E0E0; -fx-border-radius: 10; -fx-cursor: hand;");
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #F0F8FF; -fx-background-radius: 10; -fx-border-color: #3498DB; -fx-border-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(52,152,219,0.3), 10, 0, 0, 0);"));
            btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #E0E0E0; -fx-border-radius: 10; -fx-cursor: hand;"));
            btn.setOnAction(event -> addToCart(p.getProductId(), p.getProductName(), p.getPrice()));
        }
        productContainer.getChildren().add(btn);
    }

    private void addToCart(String id, String name, double price) {
        Product p = productMap.get(id);
        int currentQtyInCart = 0; CartItem existingItem = null;
        for (CartItem item : cartList) {
            if (item.getProductId().equals(id)) { currentQtyInCart = item.getQuantity(); existingItem = item; break; }
        }
        if (currentQtyInCart + 1 > p.getStockQuantity()) { showAlert(Alert.AlertType.WARNING, "Hết hàng", "Sản phẩm không đủ để thêm!"); return; }
        if (existingItem != null) existingItem.addQuantity(1); else cartList.add(new CartItem(id, name, 1, price));
        tableCart.refresh(); updateTotalAmount();
    }

    @FXML private void handleIncreaseQty() {
        CartItem selected = tableCart.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Product p = productMap.get(selected.getProductId());
            if (selected.getQuantity() + 1 > p.getStockQuantity()) { showAlert(Alert.AlertType.WARNING, "Hết hàng", "Kho đã đạt giới hạn tối đa!"); return; }
            selected.addQuantity(1); tableCart.refresh(); updateTotalAmount();
        } else showAlert(Alert.AlertType.WARNING, "Lưu ý", "Chọn món để tăng!");
    }

    @FXML private void handleDecreaseQty() {
        CartItem selected = tableCart.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (selected.getQuantity() > 1) selected.addQuantity(-1); else cartList.remove(selected);
            tableCart.refresh(); updateTotalAmount();
        } else showAlert(Alert.AlertType.WARNING, "Lưu ý", "Chọn món để giảm!");
    }

    @FXML private void handleRemoveItem() {
        CartItem selected = tableCart.getSelectionModel().getSelectedItem();
        if (selected != null) { cartList.remove(selected); tableCart.refresh(); updateTotalAmount(); }
        else showAlert(Alert.AlertType.WARNING, "Lưu ý", "Chọn món để xóa!");
    }

    private void updateTotalAmount() {
        double total = 0; for (CartItem item : cartList) total += item.getTotal();
        lblTotalAmount.setText(currencyFormat.format(total));
    }

    // ---> HÀM ĐÃ ĐƯỢC CÀI CẢM BIẾN HIỂN THỊ LỖI <---
    @FXML private void handlePayment() {
        if (cartList.isEmpty()) { showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Giỏ hàng đang trống!"); return; }

        String cusName = txtCustomerName.getText() != null && !txtCustomerName.getText().trim().isEmpty() ? txtCustomerName.getText().trim() : "Khách lẻ";
        String cusPhone = txtCustomerPhone.getText() != null ? txtCustomerPhone.getText().trim() : "";
        double total = 0; for (CartItem item : cartList) total += item.getTotal();

        String payMethodStr = cbPaymentMethod.getValue();
        String paymentCode = "Tiền mặt".equals(payMethodStr) ? "CASH" : "TRANSFER";

        if ("TRANSFER".equals(paymentCode)) {
            Image qrImage = QRCodeUtil.generateVietQR("MB", "0333186235", "TRAN CONG DAT", total, "Thanh toan don hang");
            Alert qrAlert = new Alert(Alert.AlertType.CONFIRMATION);
            qrAlert.setTitle("Thanh Toán Quét Mã");
            qrAlert.setHeaderText("Tổng tiền: " + currencyFormat.format(total) + "\nKhách dùng App Ngân hàng để quét");
            if (qrImage != null) qrAlert.setGraphic(new ImageView(qrImage));

            ButtonType btnConfirm = new ButtonType("ĐÃ NHẬN TIỀN", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnCancel = new ButtonType("HỦY GIAO DỊCH", ButtonBar.ButtonData.CANCEL_CLOSE);
            qrAlert.getButtonTypes().setAll(btnConfirm, btnCancel);

            Optional<ButtonType> result = qrAlert.showAndWait();
            if (result.isPresent() && result.get() == btnCancel) return;
        } else {
            Alert cashAlert = new Alert(Alert.AlertType.CONFIRMATION, "Thu tiền mặt: " + currencyFormat.format(total) + " VNĐ\nXác nhận khách đã đưa đủ tiền?", ButtonType.OK, ButtonType.CANCEL);
            cashAlert.setTitle("Thanh toán Tiền mặt");
            if (cashAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.CANCEL) return;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("action", "CREATE_ORDER");
        request.put("total", total);
        request.put("customerName", cusName);
        request.put("customerPhone", cusPhone);
        request.put("userId", LoginController.currentUserId);
        request.put("paymentMethod", paymentCode);
        request.put("cartItems", new ArrayList<>(cartList)); // Bọc List siêu an toàn

        String responseJson = sendToServer(request);
        if (responseJson != null) {
            Map<String, String> response = gson.fromJson(responseJson, Map.class);
            if ("SUCCESS".equals(response.get("status"))) {
                PDFExportUtil.printReceipt(new ArrayList<>(cartList), total, cusName, cusPhone, LoginController.currentUsername, payMethodStr);
                showAlert(Alert.AlertType.INFORMATION, "Hoàn tất", "Đã thanh toán và trừ kho!");
                handleClearCart(); loadProductsFromDatabase();
            } else {
                // Hiển thị tận mồm cái lỗi từ Database lên cho ní thấy!
                String errorMsg = response.containsKey("message") ? response.get("message") : "Lỗi không xác định từ Server!";
                showAlert(Alert.AlertType.ERROR, "Bắt Được Nguyên Nhân Rồi!", errorMsg);
            }
        } else showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Lỗi Server không phản hồi!");
    }

    @FXML private void handleClearCart() {
        cartList.clear(); updateTotalAmount();
        if (txtCustomerName != null) txtCustomerName.clear();
        if (txtCustomerPhone != null) txtCustomerPhone.clear();
        if (txtSearchProduct != null) txtSearchProduct.clear();
    }

    @FXML private void handleSendChat() {
        String msg = txtChatMessage.getText().trim();
        if (msg.isEmpty()) return;
        Map<String, Object> req = new HashMap<>();
        req.put("action", "SEND_CHAT"); req.put("sender", LoginController.currentUsername); req.put("message", msg);
        sendToServer(req); txtChatMessage.clear(); loadChatData();
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

                            // Nếu người gửi KHÔNG phải ADMIN (tức là NV hoặc chính mình)
                            boolean isMe = !sender.equalsIgnoreCase("ADMIN");

                            Label bubble = new Label(text);
                            bubble.setWrapText(true);
                            bubble.setMaxWidth(220);

                            javafx.scene.layout.HBox row = new javafx.scene.layout.HBox();
                            row.setPadding(new javafx.geometry.Insets(2, 0, 2, 0));

                            if (isMe) {
                                // Nhân viên gửi -> Xanh dương bên Phải
                                bubble.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 15 15 0 15; -fx-font-size: 13px;");
                                row.getChildren().add(bubble);
                                row.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                            } else {
                                // Sếp (Admin) gửi -> Xám bên Trái
                                javafx.scene.layout.VBox otherBox = new javafx.scene.layout.VBox(2);
                                Label senderLbl = new Label("Sếp (Admin)");
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

    private void initClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> lblClock.setText(LocalDateTime.now().format(formatter))), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Timeline.INDEFINITE); clock.play();
    }

    @FXML private void handleRefresh() {
        loadProductsFromDatabase(); txtSearchProduct.clear();
        showAlert(Alert.AlertType.INFORMATION, "Đã cập nhật", "Kho và giá đã được đồng bộ!");
    }

    @FXML private void handleLogout() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/bakery/view/LoginView.fxml"));
            javafx.stage.Stage stage = (javafx.stage.Stage) lblTotalAmount.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(loader.load(), 800, 500));
            stage.setTitle("Bakery Manager - Đăng Nhập"); stage.centerOnScreen(); stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }
    @FXML
    private void handleToggleChat() {
        if (chatBoxContainer != null) {
            // Kiểm tra xem nó đang mở hay đóng
            boolean isVisible = chatBoxContainer.isVisible();

            // Lật ngược trạng thái (đang đóng -> mở, đang mở -> đóng)
            chatBoxContainer.setVisible(!isVisible);
            chatBoxContainer.setManaged(!isVisible);

            // Ép nó nổi lên trên cùng (Đè lên giỏ hàng và danh sách bánh)
            if (!isVisible) {
                chatBoxContainer.toFront();
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(msg); alert.showAndWait();
    }
}