package com.test.Database_Testing;

import com.jcraft.jsch.*;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class Missing_backupcompleted {

    private final String host = "pp6.humanbrain.in";
    private final String user = "hbp";
    private final String sshPassword = "Health#123";
    private final String basePath = "/mnt/remote/tapebackup/staging";
    private final String fileName = "backupcompleted.json";

    // ✅ Email recipients
    private final String[] to = {
        "ramanan@htic.iitm.ac.in",
        "nitheshkumar.s@htic.iitm.ac.in"
    };

    private final String from = "automationsoftware25@gmail.com";
    private final String emailPassword = "wjzcgaramsqvagxu"; // Gmail App Password

    @Test
    public void checkMissingbackupcompleted.jsonFiles() {
        StringBuilder missingFolders = new StringBuilder();

        try {
            // ✅ SSH session (explicitly using jsch.Session)
            com.jcraft.jsch.Session session = new JSch().getSession(user, host, 22);
            session.setPassword(sshPassword);
            session.setConfig("StrictHostKeyChecking", "no");

            System.out.println("Connecting to SSH...");
            session.connect();

            // Get directories list
            String listDirsCmd = "ls -d " + basePath + "/*/";
            String[] dirs = executeCommand(session, listDirsCmd).split("\n");

            System.out.println("Checking for file: " + fileName);
            for (String dir : dirs) {
                if (dir.trim().isEmpty()) continue;

                // ✅ Fixed extra "/" issue
                String checkFileCmd = "[ -f " + dir + fileName + " ] && echo FOUND || echo NOT_FOUND";
                String result = executeCommand(session, checkFileCmd).trim();

                if ("NOT_FOUND".equals(result)) {
                    System.out.println("Missing in folder: " + dir);
                    missingFolders.append(dir).append("\n");
                }
            }

            session.disconnect();
            System.out.println("Done checking.");

            // Build and send email if missing folders found
            if (missingFolders.length() > 0) {
                StringBuilder emailBody = new StringBuilder();
                emailBody.append("<html><body>");
                emailBody.append("<p>The following folders are missing <b>")
                        .append(fileName).append("</b>:</p>");
                emailBody.append("<table border='1' cellspacing='0' cellpadding='5' style='border-collapse:collapse;'>");
                emailBody.append("<tr style='background-color:#f2f2f2;'>")
                        .append("<th>Sl.No</th>")
                        .append("<th>Folder Name</th>")
                        .append("</tr>");

                String[] missingList = missingFolders.toString().split("\n");
                int count = 1;
                for (String folder : missingList) {
                    if (folder.trim().isEmpty()) continue;

                    // ✅ Extract only last folder name
                    String folderName = folder.replaceAll(".*/([^/]+)/?$", "$1");

                    emailBody.append("<tr>")
                            .append("<td>").append(count++).append("</td>")
                            .append("<td>").append(folderName).append("</td>")
                            .append("</tr>");
                }

                emailBody.append("</table>");
                emailBody.append("</body></html>");

                sendEmail(to, from, emailPassword,
                        "Missing backupcompleted.json Report",
                        emailBody.toString());

                System.out.println("✅ Email sent with missing folder details (HTML table).");
            } else {
                System.out.println("✅ All folders have the file. No email sent.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            assert false : "Test failed due to exception: " + e.getMessage();
        }
    }

    // Execute command on SSH
    private String executeCommand(com.jcraft.jsch.Session session, String command) throws Exception {
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);

        try (InputStream in = channel.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

            channel.connect();

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            return output.toString();
        } finally {
            channel.disconnect();
        }
    }

    // Send email using Gmail (HTML format)
    private void sendEmail(String[] to, String from, String password, String subject, String body) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        // ✅ Fix: make them final for inner class
        final String finalFrom = from;
        final String finalPassword = password;

        javax.mail.Session mailSession = javax.mail.Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(finalFrom, finalPassword);
            }
        });

        try {
            Message message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(from));

            // Convert array into InternetAddress list
            InternetAddress[] recipientAddresses = new InternetAddress[to.length];
            for (int i = 0; i < to.length; i++) {
                recipientAddresses[i] = new InternetAddress(to[i]);
            }
            message.setRecipients(Message.RecipientType.TO, recipientAddresses);

            message.setSubject(subject);
            message.setContent(body, "text/html; charset=utf-8");

            Transport.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
