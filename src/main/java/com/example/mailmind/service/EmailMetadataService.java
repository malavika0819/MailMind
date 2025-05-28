package com.example.minder.service;

import com.example.minder.model.EmailMetadata;
import com.example.minder.model.User;
import com.example.minder.repository.EmailMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmailMetadataService {

    private static final Logger logger = LoggerFactory.getLogger(EmailMetadataService.class);
    private final EmailMetadataRepository metadataRepository;
    private final UserService userService;

    // Optional if this is the only constructor
    public EmailMetadataService(EmailMetadataRepository metadataRepository, UserService userService) {
        this.metadataRepository = metadataRepository;
        this.userService = userService;
    }

    @Transactional
    public EmailMetadata setScheduleAndPriority(Long userId, String gmailMessageId, LocalDateTime reminderDateTime, String priority, String notes, String subject, String sender) {
        logger.debug("Setting schedule and priority for userId: {}, gmailMessageId: {}, reminderTime: {}, priority: {}",
                userId, gmailMessageId, reminderDateTime, priority);

        User user = userService.findById(userId)
                .orElseThrow(() -> {
                    logger.error("User not found with ID: {} while setting schedule and priority.", userId);
                    return new RuntimeException("User not found with ID: " + userId);
                });

        EmailMetadata metadata = metadataRepository.findByUserAndGmailMessageId(user, gmailMessageId)
                .orElseGet(() -> {
                    logger.info("No existing metadata for userId: {}, gmailMessageId: {}. Creating new.", userId, gmailMessageId);
                    if (subject == null || sender == null) {
                        logger.warn("Subject or sender is null while creating new metadata for gmailMessageId: {}", gmailMessageId);
                        // Consider throwing an IllegalArgumentException if subject/sender are strictly required for new entries
                    }
                    return new EmailMetadata(user, gmailMessageId, subject, sender);
                });

        metadata.setReminderDateTime(reminderDateTime);
        metadata.setPriority(priority);
        metadata.setNotes(notes);
        // Ensure subject and sender are set if it's a new metadata object,
        // or update them if they are provided and different (optional update logic)
        if (metadata.getId() == null) { // Check if it's a new, unpersisted entity
            metadata.setSubject(subject);
            metadata.setSender(sender);
        } else {
            // Optionally update subject/sender if they are provided and different from existing
            // if (subject != null && !subject.equals(metadata.getSubject())) metadata.setSubject(subject);
            // if (sender != null && !sender.equals(metadata.getSender())) metadata.setSender(sender);
        }

        EmailMetadata savedMetadata = metadataRepository.save(metadata);
        logger.info("Schedule and priority set successfully for metadata ID: {}", savedMetadata.getId());
        return savedMetadata;
    }

    @Transactional
    public EmailMetadata setPriority(Long userId, String gmailMessageId, String priority, String subject, String sender) { // Method name is now "setPriority"
        logger.debug("Setting priority for userId: {}, gmailMessageId: {}, priority: {}", userId, gmailMessageId, priority);
        User user = userService.findById(userId)
                .orElseThrow(() -> {
                    logger.error("User not found with ID: {} while setting priority.", userId);
                    return new RuntimeException("User not found with ID: " + userId);
                });

        // This logic finds existing metadata or prepares a new one if it doesn't exist.
        // It requires subject and sender for new metadata creation.
        EmailMetadata metadata = metadataRepository.findByUserAndGmailMessageId(user, gmailMessageId)
                .orElseGet(() -> {
                    logger.info("No existing metadata for priority update via setPriority. Creating new for userId: {}, gmailMessageId: {}", userId, gmailMessageId);
                     if (subject == null || sender == null) {
                        // If subject/sender are truly optional for JUST a priority update on an existing record,
                        // this path might need adjustment, or the controller should always send them.
                        // For now, creating new metadata needs them.
                        logger.warn("Subject or sender is null while attempting to create new metadata via setPriority for gmailMessageId: {}", gmailMessageId);
                        // Or throw new IllegalArgumentException("Subject and Sender are required to create new metadata.");
                    }
                    return new EmailMetadata(user, gmailMessageId, subject, sender);
                });

        metadata.setPriority(priority);
        // If it was a new record, subject and sender were set by the constructor.
        // If it was an existing record, only priority is explicitly updated here.
        EmailMetadata savedMetadata = metadataRepository.save(metadata);
        logger.info("Priority set successfully for metadata ID: {}", savedMetadata.getId());
        return savedMetadata;
    }


    public List<EmailMetadata> getEmailMetadataForUser(Long userId) {
        logger.debug("Fetching all email metadata for userId: {}", userId);
        if (!userService.findById(userId).isPresent()) { // Ensure user exists
             logger.error("User not found with ID: {} while fetching metadata.", userId);
            throw new RuntimeException("User not found with ID: " + userId);
        }
        return metadataRepository.findByUserId(userId);
    }

    public Optional<EmailMetadata> getEmailMetadata(Long userId, String gmailMessageId) {
        logger.debug("Fetching specific email metadata for userId: {}, gmailMessageId: {}", userId, gmailMessageId);
         if (!userService.findById(userId).isPresent()) { // Ensure user exists
            logger.warn("Attempted to get metadata for non-existent userId: {}", userId);
            return Optional.empty(); // Or throw UserNotFoundException
        }
        return metadataRepository.findByUserIdAndGmailMessageId(userId, gmailMessageId);
    }

    public List<Map<String, Object>> enrichEmailsWithMetadata(Long userId, List<Map<String, Object>> gmailEmails) {
        logger.debug("Enriching {} emails with metadata for userId: {}", gmailEmails != null ? gmailEmails.size() : 0, userId);
        if (gmailEmails == null || gmailEmails.isEmpty()) {
            return Collections.emptyList();
        }
        // Ensure user exists before proceeding
        if (!userService.findById(userId).isPresent()) {
            logger.error("User not found with ID: {} during email enrichment.", userId);
            // Depending on desired behavior, could throw error or return emails un-enriched.
            // For now, returning them un-enriched after logging, but throwing might be better.
            return gmailEmails.stream().peek(emailMap -> emailMap.putIfAbsent("currentPriority", "none")).collect(Collectors.toList());
        }

        List<EmailMetadata> userMetadataList = metadataRepository.findByUserId(userId);
        Map<String, EmailMetadata> metadataMap = userMetadataList.stream()
                .collect(Collectors.toMap(EmailMetadata::getGmailMessageId, meta -> meta, (meta1, meta2) -> meta1)); // Keep first in case of theoretical duplicates

        return gmailEmails.stream().map(emailMap -> {
            String gmailId = (String) emailMap.get("id");
            if (gmailId == null) {
                logger.warn("Email map found without an 'id' field during enrichment: {}", emailMap);
                emailMap.putIfAbsent("currentPriority", "none");
                return emailMap;
            }
            EmailMetadata storedMeta = metadataMap.get(gmailId);

            if (storedMeta != null) {
                emailMap.put("currentPriority", storedMeta.getPriority());
                emailMap.put("reminderDateTime", storedMeta.getReminderDateTime());
                emailMap.put("notes", storedMeta.getNotes());
                if (storedMeta.getSubject() != null) emailMap.put("subject", storedMeta.getSubject());
                if (storedMeta.getSender() != null) emailMap.put("sender", storedMeta.getSender());
            } else {
                emailMap.put("currentPriority", "none");
            }
            return emailMap;
        }).collect(Collectors.toList());
    }

    public List<EmailMetadataRepository.EmailReminderDetails> getUpcomingReminderDetails(Long userId) {
        logger.debug("Fetching upcoming reminder details (projection) for userId: {}", userId);
        if (!userService.findById(userId).isPresent()) {
            logger.error("User not found with ID: {} while fetching upcoming reminder details.", userId);
            throw new RuntimeException("User not found with ID: " + userId);
        }
        return metadataRepository.findUpcomingReminderDetailsForUser(userId, LocalDateTime.now());
    }

   public List<EmailMetadata> getUpcomingReminders(Long userId) {
        logger.debug("Fetching upcoming reminders (full objects) for userId: {}", userId);
        if (!userService.findById(userId).isPresent()) {
            logger.error("User not found with ID: {} for upcoming reminders (full).", userId);
            throw new RuntimeException("User not found with ID: " + userId);
        }
        return metadataRepository.findByUserIdAndReminderDateTimeIsNotNull(userId)
                .stream()
                .filter(meta -> meta.getReminderDateTime() != null && meta.getReminderDateTime().isAfter(LocalDateTime.now()))
                .sorted((m1, m2) -> m1.getReminderDateTime().compareTo(m2.getReminderDateTime()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteEmailMetadata(Long metadataId) {
        logger.info("Deleting email metadata with ID: {}", metadataId);
        if (!metadataRepository.existsById(metadataId)) {
            logger.warn("EmailMetadata with ID: {} not found for deletion.", metadataId);
            return; // Or throw an exception e.g., ResourceNotFoundException
        }
        metadataRepository.deleteById(metadataId);
        logger.info("EmailMetadata with ID: {} deleted.", metadataId);
    }
}