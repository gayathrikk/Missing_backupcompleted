package com.test.Database_Testing;

import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class QC_Pending_Batches {

    @Test
    public void testDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("Driver loaded");

            String url = "jdbc:mysql://dev2kamal.humanbrain.in:3306/HBA_V2";
            String username = "root";
            String password = "Health#123";
            Connection connection = DriverManager.getConnection(url, username, password);
            System.out.println("MYSQL database connected");

            executeAndPrintQuery(connection);
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void executeAndPrintQuery(Connection connection) {
        try {
            Statement statement = connection.createStatement();
            String query = "SELECT id, name, datalocation, arrival_date, totalImages " +
                           "FROM `slidebatch` " +
                           "WHERE (process_status = 6 OR process_status = 11) " +
                           "AND `arrival_date` < DATE_SUB(CURDATE(), INTERVAL 1 DAY);";

            ResultSet resultSet = statement.executeQuery(query);

            int IdWidth = 10;
            int nameWidth = 40;
            int datalocationWidth = 30;
            int arrival_dateWidth = 20;
            int totalImagesWidth = 20;
            int daysWidth = 15; // Width for the new column

            // Building email content
            StringBuilder emailContent = new StringBuilder();
            emailContent.append("<html><body><pre>");
            emailContent.append("<b>This is an automatically generated email,</b>\n\n");
            emailContent.append("For your attention and action:\n");
            emailContent.append("The following batches have QC pending for more than 1 day:\n\n");
            emailContent.append(String.format("%-" + IdWidth + "s %-"+ nameWidth + "s %-"+ datalocationWidth + "s %-"+ arrival_dateWidth + "s %-" + totalImagesWidth + "s %-" + daysWidth + "s%n",
                    "Id", "name", "datalocation", "arrival_date", "totalImages", "No.of days"));

            // Adding separator line
            String separatorLine = "-".repeat(IdWidth + nameWidth + datalocationWidth + arrival_dateWidth + totalImagesWidth + daysWidth);
            emailContent.append(separatorLine).append("\n");

            boolean dataFound = false;

            while (resultSet.next()) {
                dataFound = true;
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                String datalocation = resultSet.getString("datalocation");
                String arrivalDateStr = resultSet.getString("arrival_date"); // Assuming arrival_date is stored as a String
                int totalImages = resultSet.getInt("totalImages");

                LocalDate arrivalDate = LocalDate.parse(arrivalDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                long daysDifference = ChronoUnit.DAYS.between(arrivalDate, LocalDate.now());

                emailContent.append(String.format("%-" + IdWidth + "d %-" + nameWidth + "s %-" + datalocationWidth + "s %-" + arrival_dateWidth + "s %-" + totalImagesWidth + "d %-" + daysWidth + "d%n",
                        id, name, datalocation, arrivalDateStr, totalImages, daysDifference));
            }

            emailContent.append("</pre></body></html>");

            // Close the statement
            resultSet.close();
            statement.close();

            if (dataFound) {
                sendEmailAlert(emailContent.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendEmailAlert(String messageBody) {
        // Recipient's email ID needs to be mentioned.
    	String[] to = {"karthik6595@gmail.com" , "sindhu.r@htic.iitm.ac.in"};
        String[] cc = {"richavermaj@gmail.com", "nathan.i@htic.iitm.ac.in", "divya.d@htic.iitm.ac.in", "venip@htic.iitm.ac.in"};
        String[] bcc = {};

        // Sender's email ID needs to be mentioned
        String from = "gayathri@htic.iitm.ac.in";

        // Assuming you are sending email from through gmails smtp
        String host = "smtp.gmail.com";

        // Get system properties
        Properties properties = System.getProperties();

        // Setup mail server
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");

        // Get the Session object and pass username and password
        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("gayathri@htic.iitm.ac.in", "Gayu@0918");
            }
        });

        // Used to debug SMTP issues
        session.setDebug(true);

        try {
            // Create a default MimeMessage object.
            MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            message.setFrom(new InternetAddress(from));

            // Set To: header field of the header.
            for (String recipient : to) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            }

            // Set CC: header field of the header, if any.
//            for (String ccRecipient : cc) {
//                message.addRecipient(Message.RecipientType.CC, new InternetAddress(ccRecipient));
//            }

            // Set BCC: header field of the header, if any.
            for (String bccRecipient : bcc) {
                message.addRecipient(Message.RecipientType.BCC, new InternetAddress(bccRecipient));
            }

            // Set Subject: header field
            message.setSubject("Scanning Pipeline: Image QC: Alert");

            // Set the actual message
            message.setContent(messageBody, "text/html");

            System.out.println("sending...");
            // Send message
            Transport.send(message);
            System.out.println("Sent message successfully....");
        } catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }
}
