package com.test.Database_Testing;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.testng.annotations.Test;

public class demo {

    @Test
    public void testDB() {
        Connection connection = null;
        try {
            // Establish database connection
            connection = getConnection();
            if (connection != null) {
                System.out.println("MySQL database connected");

                // Get slidebatch ID from user input
                Scanner scanner = new Scanner(System.in);
                System.out.println("Enter the slidebatch ID:");
                int slidebatchId = scanner.nextInt();

                // Execute the query and collect results
                List<Map<String, Object>> queryResults = executeAndCollectQueryResults(connection, slidebatchId);

                // Print the query results
                printQueryResults(queryResults);

            } else {
                System.out.println("Failed to connect to the database.");
            }

        } catch (SQLException e) {
            System.out.println("Database error:");
            e.printStackTrace();
        } finally {
            // Close the connection in finally block to ensure it's always closed
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    System.out.println("Connection closed.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private Connection getConnection() throws SQLException {
        // Load the MySQL JDBC driver
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL Driver loaded");
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL JDBC driver not found.");
            e.printStackTrace();
            throw new SQLException("JDBC driver not found", e);
        }

        // Connect to the MySQL database
        String url = "jdbc:mysql://apollo2.humanbrain.in:3306/HBA_V2";
        String username = "root";
        String password = "Health#123";
        return DriverManager.getConnection(url, username, password);
    }

    private List<Map<String, Object>> executeAndCollectQueryResults(Connection connection, int slidebatchId) throws SQLException {
        List<Map<String, Object>> queryResults = new ArrayList<>();
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connection.createStatement();
            String query = "SELECT slidebatch.id, slide.filename, huron_slideinfo.isQC FROM slidebatch "
                         + "LEFT JOIN slide ON slide.slidebatch = slidebatch.id "
                         + "LEFT JOIN huron_slideinfo ON huron_slideinfo.slide = slide.id "
                         + "WHERE slidebatch.id = " + slidebatchId;

            resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                Map<String, Object> result = new HashMap<>();
                result.put("id", resultSet.getInt("id"));
                result.put("filename", resultSet.getString("filename"));
                result.put("isQC", resultSet.getBoolean("isQC"));

                String filename = resultSet.getString("filename");
                // Extract the biosample value
                String biosample = filename.substring(filename.indexOf("B_") + 2,
                        filename.indexOf('_', filename.indexOf("B_") + 2));
                result.put("biosample", biosample);

                // Extract the series value
                String series = filename.substring(filename.indexOf("ST_") + 3,
                        filename.indexOf('-', filename.indexOf("ST_") + 3));
                result.put("series", series);

                // Extract the section value
                String section = filename.substring(filename.indexOf("SE_") + 3,
                        filename.lastIndexOf('.'));
                result.put("section", section);

                queryResults.add(result);
            }

        } finally {
            // Close resources in finally block
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
        }
        return queryResults;
    }

    private void printQueryResults(List<Map<String, Object>> queryResults) {
        int qcTrueCount = 0;
        int qcFalseCount = 0;

        for (Map<String, Object> result : queryResults) {
            boolean isQC = (boolean) result.get("isQC");
            if (isQC) {
                qcTrueCount++;
            } else {
                qcFalseCount++;
            }
        }

        // Print summary
        System.out.println("Total number of sections: " + queryResults.size());
        System.out.println("Total number of QC passed sections: " + qcTrueCount);
        System.out.println("Total number of QC failed sections: " + qcFalseCount);
        System.out.println("Query Result:");
        System.out.println("--------------------------------------------------------------------------------------------------------");
        System.out.printf("%-10s | %-10s | %-10s | %-10s | %-45s | %-10s%n", "ID", "Biosample", "Series", "Section", "Filename", "isQC");
        System.out.println("--------------------------------------------------------------------------------------------------------");

        // Print each result
        for (Map<String, Object> result : queryResults) {
            System.out.printf("%-10d | %-10s | %-10s | %-10s | %-45s | %-10b%n",
                    (int) result.get("id"),
                    (String) result.get("biosample"),
                    (String) result.get("series"),
                    (String) result.get("section"),
                    (String) result.get("filename"),
                    (boolean) result.get("isQC"));
        }
        System.out.println("--------------------------------------------------------------------------------------------------------");
    }
}
