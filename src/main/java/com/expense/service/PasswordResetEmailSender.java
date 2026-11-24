package com.expense.service;

import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class PasswordResetEmailSender {

    public void sendResetEmail(String recipientEmail, String resetUrl) throws Exception {
        String host = envOrDefault("FINSIGHT_SMTP_HOST", "smtp.gmail.com");
        String port = envOrDefault("FINSIGHT_SMTP_PORT", "587");
        String username = System.getenv("FINSIGHT_SMTP_USERNAME");
        String password = System.getenv("FINSIGHT_SMTP_PASSWORD");
        String from = envOrDefault("FINSIGHT_SMTP_FROM", username);

        if (username == null || username.isBlank()
                || password == null || password.isBlank()
                || from == null || from.isBlank()) {
            throw new IllegalStateException("SMTP credentials are not configured.");
        }

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
        message.setSubject("FinSight password reset");
        message.setText(
                "We received a request to reset your FinSight password.\n\n"
                        + "Open this link within 30 minutes:\n"
                        + resetUrl + "\n\n"
                        + "If you did not request this reset, ignore this email.",
                "UTF-8");

        Transport.send(message);
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value != null && !value.isBlank() ? value : fallback;
    }
}
