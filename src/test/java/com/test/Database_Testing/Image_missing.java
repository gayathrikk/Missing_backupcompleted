package com.test.Database_Testing;

import com.jcraft.jsch.*;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.*;

public class Image_missing {

    @Test
    public void testDBandListRemoteFiles() {
        // Step 1: Connect to MySQL Database and retrieve biosample-series-section mappings
        Map<Integer, Map<String, List<Integer>>> biosampleSeriesSections = connectAndQueryDB();

        // Step 2: SSH Connection and list files using grep filter
        String host = "pp6.humanbrain.in";
        String user = "hbp";
        String password = "Health#123";
        String basePath = "/lustre/data/store10PB/repos1/iitlab/humanbrain/analytics";

        listRemoteFiles(host, user, password, basePath, biosampleSeriesSections);
    }

    private Map<Integer, Map<String, List<Integer>>> connectAndQueryDB() {
        Map<Integer, Map<String, List<Integer>>> biosampleSeriesSections = new HashMap<>();

        String url = "jdbc:mysql://apollo2.humanbrain.in:3306/HBA_V2";
        String username = "root";
        String password = "Health#123";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("Driver loaded");

            try (Connection connection = DriverManager.getConnection(url, username, password)) {
                System.out.println("MySQL database connected");
                biosampleSeriesSections = executeAndPrintQuery(connection);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
        }
        return biosampleSeriesSections;
    }

    private Map<Integer, Map<String, List<Integer>>> executeAndPrintQuery(Connection connection) {
        String query = "SELECT b.id AS biosample, sr.name AS series_name, s.positionindex AS section_no " +
                "FROM section s " +
                "INNER JOIN series sr ON s.series = sr.id " +
                "INNER JOIN seriesset ss ON sr.seriesset = ss.id " +
                "INNER JOIN biosample b ON ss.biosample = b.id " +
                "WHERE DATE(s.created_ts) = DATE_SUB(CURDATE(), INTERVAL 4 DAY)";

        Map<Integer, Map<String, List<Integer>>> biosampleSeriesSections = new HashMap<>();

        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            boolean dataFound = false;
            System.out.printf("%-20s %-20s %-10s%n", "Biosample", "Series Name", "Section No");
            System.out.println("-".repeat(50));

            while (resultSet.next()) {
                dataFound = true;

                int biosample = resultSet.getInt("biosample");
                String seriesName = resultSet.getString("series_name");
                int sectionNo = resultSet.getInt("section_no");

                System.out.printf("%-20d %-20s %-10d%n", biosample, seriesName, sectionNo);

                // Extract series suffix dynamically
                String suffix = seriesName.contains("_") ? seriesName.split("_", 2)[1] : seriesName;

                biosampleSeriesSections
                        .computeIfAbsent(biosample, k -> new HashMap<>())
                        .computeIfAbsent(suffix, k -> new ArrayList<>())
                        .add(sectionNo);
            }

            if (!dataFound) {
                System.out.println("No records found for the specified date.");
            }

        } catch (SQLException e) {
            System.err.println("SQL query execution error: " + e.getMessage());
        }
        return biosampleSeriesSections;
    }

    private void listRemoteFiles(String host, String user, String password, String basePath, Map<Integer, Map<String, List<Integer>>> biosampleSeriesSections) {
        Session session = null;
        ChannelExec channelExec = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(user, host, 22);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");

            try {
                session.connect();
                System.out.println("\nConnected to " + host);
            } catch (JSchException e) {
                System.err.println("Failed to connect to SSH: " + e.getMessage());
                return;
            }

            for (Map.Entry<Integer, Map<String, List<Integer>>> entry : biosampleSeriesSections.entrySet()) {
                int biosample = entry.getKey();

                for (Map.Entry<String, List<Integer>> seriesEntry : entry.getValue().entrySet()) {
                    String suffix = seriesEntry.getKey(); // Extracted dynamically
                    String remotePath = basePath + "/" + biosample + "/" + suffix;

                    for (int sectionNo : seriesEntry.getValue()) {
                        String command = "ls " + remotePath + " | grep _" + sectionNo + "_";

                        try {
                            channelExec = (ChannelExec) session.openChannel("exec");
                            channelExec.setCommand(command);
                            channelExec.setInputStream(null);
                            channelExec.setErrStream(System.err);

                            InputStream input = channelExec.getInputStream();
                            channelExec.connect();

                            System.out.println("\nFiltered files in: " + remotePath + " for section " + sectionNo);
                            Scanner scanner = new Scanner(input);
                            boolean fileFound = false;
                            while (scanner.hasNextLine()) {
                                fileFound = true;
                                System.out.println(" - " + scanner.nextLine());
                            }
                            if (!fileFound) {
                                System.out.println(" - No files found for section " + sectionNo);
                            }
                            scanner.close();
                        } catch (JSchException | IOException e) {
                            System.err.println("Error executing command: " + command + " - " + e.getMessage());
                        } finally {
                            if (channelExec != null) {
                                channelExec.disconnect();
                            }
                        }
                    }
                }
            }
        } catch (JSchException e) {
            System.err.println("SSH Connection error: " + e.getMessage());
        } finally {
            if (session != null) {
                session.disconnect();
            }
            System.out.println("SSH Connection closed.");
        }
    }
}
