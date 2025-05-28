package com.example.minder.service;

import com.example.minder.model.EmailMetadata;
import com.example.minder.repository.EmailMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationSchedulerService.class);

    @Autowired
    private EmailMetadataRepository metadataRepository;

    @Autowired
    private EmailSendingService emailSendingService;

    @Scheduled(cron = "0 * * * * ?") // Runs at the start of every minute
    @Transactional
    public void checkForDueRemindersAndSendNotifications() {
        logger.debug("Checking for due reminders at {}", LocalDateTime.now());
        LocalDateTime now = LocalDateTime.now();
        // Query needs to consider a small window for reminders slightly past due
        List<EmailMetadata> dueReminders = metadataRepository.findDueAndUnnotifiedReminders(now);

        if (dueReminders.isEmpty()) {
            return;
        }
        logger.info("Found {} due reminders to process.", dueReminders.size());
        for (EmailMetadata reminder : dueReminders) {
            try {
                logger.info("Processing reminder for emailId: {}, subject: '{}', for user: {}",
                        reminder.getGmailMessageId(), reminder.getSubject(), reminder.getUser().getEmail());
                String recipientEmail = reminder.getUser().getEmail();
                String emailSubject = "MailMinder Reminder: " + reminder.getSubject();
                String emailBody = "Hi " + (reminder.getUser().getDisplayName() != null ? reminder.getUser().getDisplayName() : "there") + ",\n\n" +
                                 "This is a reminder from MailMinder regarding your email:\n" +
                                 "Subject: " + reminder.getSubject() + "\n" +
                                 "From: " + reminder.getSender() + "\n" +
                                 (reminder.getNotes() != null && !reminder.getNotes().isEmpty() ? "Your Notes: " + reminder.getNotes() + "\n" : "") +
                                 "\nIt was scheduled for: " + reminder.getReminderDateTime().toLocalDate() + " at " + reminder.getReminderDateTime().toLocalTime() +
                                 "\n\nThanks,\nThe MailMinder Team";
                emailSendingService.sendSimpleMessage(recipientEmail, emailSubject, emailBody);
                reminder.setNotified(true);
                metadataRepository.save(reminder);
                logger.info("Notification sent for reminder ID: {}, emailId: {}", reminder.getId(), reminder.getGmailMessageId());
            } catch (Exception e) {
                logger.error("Failed to send notification for reminder ID: {}, emailId: {}: {}",
                             reminder.getId(), reminder.getGmailMessageId(), e.getMessage(), e);
            }
        }
    }
}