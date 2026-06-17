package com.bakery.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:sqlserver://localhost:63443;"
            + "databaseName=bakerydb;"
            + "integratedSecurity=true;"
            + "encrypt=true;trustServerCertificate=true;"
            + "sendStringParametersAsUnicode=true;";

    private static Connection connection = null;

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

                connection = DriverManager.getConnection(URL);
                System.out.println("Kết nối Database SQL Server thành công!");
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            System.err.println("Lỗi kết nối Database SQL Server!");
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Ngắt kết nối SQL Server thành công!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}