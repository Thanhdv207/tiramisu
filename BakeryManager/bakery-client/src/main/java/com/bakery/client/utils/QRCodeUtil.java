package com.bakery.client.utils;

import javafx.scene.image.Image;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class QRCodeUtil {

    // Gọi API VietQR để tạo mã quét tự động điền số tiền
    public static Image generateVietQR(String bankId, String accountNo, String accountName, double amount, String description) {
        try {
            // Đã sửa lại thành UTF_8 (dấu gạch dưới) và bỏ .toString() cho gọn gàng
            String encodedDesc = URLEncoder.encode(description, StandardCharsets.UTF_8);
            String encodedName = URLEncoder.encode(accountName, StandardCharsets.UTF_8);

            // Tạo link API chuẩn của VietQR
            String url = String.format("https://img.vietqr.io/image/%s-%s-compact2.png?amount=%.0f&addInfo=%s&accountName=%s",
                    bankId, accountNo, amount, encodedDesc, encodedName);

            // JavaFX hỗ trợ tải ảnh trực tiếp từ URL Internet
            return new Image(url, 350, 350, true, true);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}