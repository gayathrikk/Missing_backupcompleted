package com.test.Database_Testing;

import com.jcraft.jsch.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class Missing_backupcompleted {
    public static void main(String[] args) {
        String host = "pp6.humanbrain.in";
        String user = "hbp";
        String password = "Health#123";
        String basePath = "/mnt/remote/tapebackup/staging";
        String fileName = "backupcompleted.json";

        // Email credentials
        String to = "ramanan@htic.iitm.ac.in";
        String from = "automationsoftware25@gmail.com";
        String emailPassword = "wjzcgaramsqvagxu"; // Gmail App Password

        StringBuilder missingFolders = new StringBuilder();

        try {
            // SSH session
            com.jcraft.jsch.Session session = new JSch().getSession(user, host, 22);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");

            System.out.println("Connecting to SSH...");
            session.connect();

            // Get directories list
            String listDirsCmd = "ls -d " + basePath + "/*/";
            String[] dirs = executeCommand(session, listDirsCmd).split("\n");

            System.out.println("Checking for file: " + fileName);
            for (String dir : dirs) {
                if (dir.trim().isEmpty()) continue;

                String checkFileCmd = "[ -f " + dir + "/" + fileName + " ] && echo FOUND || echo NOT_FOUND";
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
                emailBody.append("<table border='1' cellspacing='0' cellpadding='5'>");
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
                        "Missing chunk_assignments.json Report",
                        emailBody.toString());

                System.out.println("Email sent with missing folder details (HTML table).");
            } else {
                System.out.println("All folders have the file. No email sent.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Execute command on SSH
    private static String executeCommand(com.jcraft.jsch.Session session, String command) throws Exception {
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);

        InputStream in = channel.getInputStream();
        channel.connect();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        channel.disconnect();
        return output.toString();
    }

    // Send email using Gmail (HTML format)
    private static void sendEmail(String to, String from, String password, String subject, String body) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        javax.mail.Session mailSession = javax.mail.Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        try {
            Message message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);

            // ✅ Set HTML body
            message.setContent(body, "text/html; charset=utf-8");

            Transport.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
