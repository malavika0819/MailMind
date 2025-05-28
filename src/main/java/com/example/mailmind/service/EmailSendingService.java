package com.example.minder.service; // Ensure this matches your actual package structure
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value; // For injecting property value directly
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
@Service
public class EmailSendingService {
private static final Logger logger = LoggerFactory.getLogger(EmailSendingService.class);

private final JavaMailSender mailSender;
// private final Environment env; // Option 1: Using Environment

@Value("${spring.mail.username}") // Option 2: Directly inject the property
private String mailFromAddress;

public EmailSendingService(JavaMailSender mailSender /*, Environment env */) { // Remove env if using @Value
    this.mailSender = mailSender;
    // this.env = env; // Remove if using @Value
}

public void sendSimpleMessage(String to, String subject, String text) {
    try {
        SimpleMailMessage message = new SimpleMailMessage();

        // String fromAddress = env.getProperty("spring.mail.username"); // Option 1
        String fromAddress = this.mailFromAddress; // Option 2 (using @Value)

        if (fromAddress == null || fromAddress.trim().isEmpty()) {
            logger.error("Sender email ('spring.mail.username') is not configured in application.properties.");
            throw new IllegalStateException("Sender email address is not configured.");
        }
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text); // Use setText for the body content

        mailSender.send(message);
        logger.info("Email sent successfully to: {}", to);

    } catch (MailException e) { // Catch Spring's specific MailException for better context
        logger.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        // You might want to throw a custom application exception here or handle it more gracefully
        throw new RuntimeException("Email sending failed for recipient " + to, e);
    } catch (IllegalStateException e) { // Catch our own configuration error
         logger.error("Email sending aborted due to configuration error: {}", e.getMessage());
        throw e;
    }
}
}