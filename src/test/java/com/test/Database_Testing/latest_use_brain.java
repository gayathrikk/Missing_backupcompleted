package com.test.Database_Testing;

import java.sql.*;

public class latest_use_brain {

    public static void main(String[] args) {
        String url = "jdbc:mysql://apollo2.humanbrain.in:3306/HBA_V2";
        String username = "root";
        String password = "Health#123";

        String sql = "SELECT \r\n"
                + "    activity.id,\r\n"
                + "    activity.timestamp,\r\n"
                + "    activity.action,\r\n"
                + "    activity.info,\r\n"
                + "    CC_User.user_name,\r\n"
                + "    seriesset.name AS seriesset_name\r\n"
                + "FROM activity\r\n"
                + "JOIN CC_User ON CC_User.id = activity.user\r\n"
                + "JOIN seriesset ON seriesset.id = CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(activity.info, ':', 1), '-', -1) AS UNSIGNED)\r\n"
                + "WHERE DATE(activity.timestamp) = CURDATE() - INTERVAL 1 DAY\r\n"
                + "  AND activity.action != 'Login'\r\n"
                + "  AND activity.info LIKE 'SS-%'\r\n"
                + "ORDER BY seriesset.name, activity.info ASC;";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, username, password);
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            // Consistent column format
            String format = "%-15s %-25s %-25s %-30s %-25s %-30s%n";

            // Print headers
            System.out.printf(format, "id", "timestamp", "action", "info", "user_name", "name");
            System.out.println("-".repeat(150));

            String lastSeriesName = null;

            // Print data rows
            while (rs.next()) {
                String currentSeriesName = rs.getString("seriesset_name");

                if (lastSeriesName != null && !lastSeriesName.equals(currentSeriesName)) {
                    System.out.println(); // Space between seriesset.name groups
                }
                lastSeriesName = currentSeriesName;

                System.out.printf(format,
                        rs.getString("id"),
                        rs.getString("timestamp"),
                        rs.getString("action"),
                        rs.getString("info"),
                        rs.getString("user_name"),
                        currentSeriesName);
            }

            rs.close();
            stmt.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
