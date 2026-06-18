package com.bakery.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Tải giao diện Login từ thư mục resources
        Parent root = FXMLLoader.load(getClass().getResource("/com/bakery/view/LoginView.fxml"));

        Scene scene = new Scene(root, 800, 500);

        primaryStage.setTitle("Bakery Manager - Đăng Nhập");
        primaryStage.setResizable(false); // Khóa phóng to thu nhỏ ở màn hình đăng nhập
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}