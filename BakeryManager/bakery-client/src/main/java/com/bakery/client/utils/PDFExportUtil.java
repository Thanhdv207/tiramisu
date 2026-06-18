package com.bakery.client.utils;

import com.bakery.shared.model.CartItem;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class PDFExportUtil {

    // Nạp thêm tên thu ngân vào hàm
    public static void printReceipt(List<CartItem> cartList, double totalAmount, String customerName, String customerPhone, String cashierName, String paymentMethod) {        Document document = new Document();
        try {
            File dir = new File("receipts");
            if (!dir.exists()) dir.mkdirs();

            String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String fileName = "receipts/HoaDon-" + timeStamp + ".pdf";

            PdfWriter.getInstance(document, new FileOutputStream(fileName));
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

            Paragraph title = new Paragraph("BAKERY RECEIPT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), normalFont));

            // --- IN TÊN THU NGÂN ---
            document.add(new Paragraph("Cashier: " + removeAccents(cashierName != null ? cashierName : "N/A"), normalFont));
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("Customer: " + removeAccents(customerName), boldFont));
            document.add(new Paragraph("Payment Method: " + removeAccents(paymentMethod), normalFont));
            if (customerPhone != null && !customerPhone.isEmpty()) {
                document.add(new Paragraph("Phone: " + customerPhone, normalFont));
            }

            document.add(new Paragraph("-------------------------------------------------------------------", normalFont));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4f, 1f, 2.5f, 2.5f});

            table.addCell(new PdfPCell(new Phrase("Product Name", boldFont)));
            table.addCell(new PdfPCell(new Phrase("Qty", boldFont)));
            table.addCell(new PdfPCell(new Phrase("Price", boldFont)));
            table.addCell(new PdfPCell(new Phrase("Total", boldFont)));

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

            for (CartItem item : cartList) {
                table.addCell(new Phrase(removeAccents(item.getProductName()), normalFont));
                table.addCell(new Phrase(String.valueOf(item.getQuantity()), normalFont));
                table.addCell(new Phrase(currencyFormat.format(item.getUnitPrice()), normalFont));
                table.addCell(new Phrase(currencyFormat.format(item.getTotal()), normalFont));
            }
            document.add(table);

            document.add(Chunk.NEWLINE);
            Paragraph totalPara = new Paragraph("TOTAL AMOUNT: " + currencyFormat.format(totalAmount), titleFont);
            totalPara.setAlignment(Element.ALIGN_RIGHT);
            document.add(totalPara);

            document.close();

            File pdfFile = new File(fileName);
            if (pdfFile.exists() && java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(pdfFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String removeAccents(String text) {
        String temp = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        return temp.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").replace('đ', 'd').replace('Đ', 'D');
    }
}
