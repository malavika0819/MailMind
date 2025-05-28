package com.example.minder.controller;
import com.example.minder.dto.EnrichedEmailDto;
import com.example.minder.dto.ScheduleAndPriorityRequest; // MODIFIED: Using new DTO
import com.example.minder.model.EmailMetadata;
import com.example.minder.model.User;
import com.example.minder.service.EmailMetadataService;
import com.example.minder.service.GmailService;
import com.example.minder.service.UserService;
import com.example.minder.dto.PriorityUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://localhost:8080"}, allowCredentials = "true")
public class EmailController {
private static final Logger logger = LoggerFactory.getLogger(EmailController.class);

private final GmailService gmailService;
private final EmailMetadataService metadataService;
private final UserService userService;
private final OAuth2AuthorizedClientService authorizedClientService;

public EmailController(GmailService gmailService,
                       EmailMetadataService metadataService,
                       UserService userService,
                       OAuth2AuthorizedClientService authorizedClientService) {
    this.gmailService = gmailService;
    this.metadataService = metadataService;
    this.userService = userService;
    this.authorizedClientService = authorizedClientService;
}

private Long getInternalUserId(OAuth2User principal) {
    if (principal == null) {
        logger.warn("getInternalUserId called with null principal.");
        throw new IllegalStateException("User not authenticated.");
    }
    String userEmail = principal.getAttribute("email");
    if (userEmail == null) {
        logger.warn("Email attribute not found in principal: {}", principal.getAttributes());
        throw new IllegalStateException("Email not available from authentication provider.");
    }
    User appUser = userService.findByEmail(userEmail)
            .orElseGet(() -> {
                logger.warn("Authenticated user with email {} not found in local DB. Creating/updating. This path should ideally be hit only once per user by CustomOAuth2UserService.", userEmail);
                String googleId = principal.getName();
                String displayName = principal.getAttribute("name");
                return userService.createOrUpdateUser(googleId, userEmail, displayName);
            });
    return appUser.getId();
}

private String getGoogleAccessToken(OAuth2AuthenticationToken authentication) {
    if (authentication == null) {
        logger.warn("getGoogleAccessToken called with null authentication token.");
        throw new IllegalStateException("User authentication token not available.");
    }
    OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
            authentication.getAuthorizedClientRegistrationId(),
            authentication.getName()
    );
    if (client == null || client.getAccessToken() == null) {
        logger.error("Could not obtain OAuth2AuthorizedClient or access token for user: {}. Reg ID: {}",
                authentication.getName(), authentication.getAuthorizedClientRegistrationId());
        throw new IllegalStateException("Could not obtain access token for Gmail API.");
    }
    return client.getAccessToken().getTokenValue();
}

@GetMapping("/emails")
public ResponseEntity<?> getEmails(OAuth2AuthenticationToken authentication, @AuthenticationPrincipal OAuth2User principal) {
    Long internalUserId = getInternalUserId(principal);
    String accessToken = getGoogleAccessToken(authentication);
    logger.info("API CALL: GET /api/emails for internalUserId: {}", internalUserId);
    try {
        List<Map<String, Object>> rawEmailsFromGmail = gmailService.fetchImportantEmails(internalUserId, accessToken);
        List<Map<String, Object>> enrichedEmailMaps = metadataService.enrichEmailsWithMetadata(internalUserId, rawEmailsFromGmail);
        List<EnrichedEmailDto> emailDtos = enrichedEmailMaps.stream()
            .map(emailMap -> {
                LocalDateTime reminderTime = null;
                Object reminderObj = emailMap.get("reminderDateTime");
                if (reminderObj instanceof LocalDateTime) {
                    reminderTime = (LocalDateTime) reminderObj;
                } else if (reminderObj instanceof String) {
                    try {
                        String dateStr = (String) reminderObj;
                        if (dateStr.endsWith("Z") && dateStr.length() == 20) {
                            reminderTime = LocalDateTime.parse(dateStr.substring(0, 19));
                        } else if (dateStr.length() == 19) {
                             reminderTime = LocalDateTime.parse(dateStr);
                        } else if (dateStr.length() == 16) {
                             reminderTime = LocalDateTime.parse(dateStr + ":00");
                        } else {
                            reminderTime = LocalDateTime.parse(dateStr);
                        }
                    } catch (DateTimeParseException e) {
                        logger.warn("Could not parse reminderDateTime string: '{}' for email ID: {}", reminderObj, emailMap.get("id"), e);
                    }
                }
                return new EnrichedEmailDto(
                    (String) emailMap.get("id"), (String) emailMap.get("subject"),
                    (String) emailMap.get("sender"), (String) emailMap.get("snippet"),
                    (String) emailMap.get("date"), (String) emailMap.get("currentPriority"),
                    reminderTime, (String) emailMap.get("notes")
                );
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(emailDtos);
    } catch (IOException | GeneralSecurityException e) {
        logger.error("API ERROR: Gmail API interaction failed for userId {}: {}", internalUserId, e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                             .body(Map.of("error", "Failed to fetch emails from Gmail. Please try again later."));
    } catch (IllegalStateException e) {
        logger.error("API ERROR: Authentication, token, or state issue for userId {}: {}", internalUserId, e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
        logger.error("API ERROR: Unexpected error fetching emails for userId {}: {}", internalUserId, e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred."));
    }
}

@PostMapping("/emails/{gmailMessageId}/set-schedule-priority") // MODIFIED ENDPOINT NAME AND DTO
public ResponseEntity<?> setScheduleAndPriority(
        @PathVariable String gmailMessageId,
        @RequestBody ScheduleAndPriorityRequest request, // MODIFIED DTO
        @AuthenticationPrincipal OAuth2User principal) {
    Long internalUserId = getInternalUserId(principal);
    logger.info("API CALL: POST /api/emails/{}/set-schedule-priority for internalUserId: {}, reminderTime: {}, priority: {}",
            gmailMessageId, internalUserId, request.getReminderDateTime(), request.getPriority());

    try {
        if (request.getReminderDateTime() == null || request.getReminderDateTime().trim().isEmpty()) {
             return ResponseEntity.badRequest().body(Map.of("error", "Reminder date and time cannot be null or empty."));
        }
        if (request.getSubject() == null || request.getSender() == null) {
             return ResponseEntity.badRequest().body(Map.of("error", "Subject and Sender are required when scheduling."));
        }
         if (request.getPriority() == null) { // Also check priority
            return ResponseEntity.badRequest().body(Map.of("error", "Priority cannot be null."));
        }

        LocalDateTime reminderTime;
        try {
            String dateTimeToParse = request.getReminderDateTime();
            if (dateTimeToParse.length() == 16) {
                dateTimeToParse += ":00";
            }
            reminderTime = LocalDateTime.parse(dateTimeToParse);
        } catch (DateTimeParseException e) {
            logger.warn("API BAD REQUEST: Invalid reminderDateTime format: '{}' for gmailMessageId: {}", request.getReminderDateTime(), gmailMessageId, e);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid reminder date-time format. Use ISO format (YYYY-MM-DDTHH:mm or YYYY-MM-DDTHH:mm:ss)."));
        }

        EmailMetadata updatedMetadata = metadataService.setScheduleAndPriority( // Assumes a new/modified service method
                internalUserId,
                gmailMessageId,
                reminderTime,
                request.getPriority(),
                request.getNotes(),
                request.getSubject(),
                request.getSender()
        );
        return ResponseEntity.ok(Map.of("message", "Email scheduled and priority set successfully", "data", updatedMetadata));
    } catch (IllegalStateException e) {
        logger.error("API ERROR: Authentication or state issue for userId {}: {}", internalUserId, e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
    } catch (RuntimeException e) {
         logger.error("API ERROR: Scheduling email for internalUserId {}, gmailMessageId {}: {}", internalUserId, gmailMessageId, e.getMessage(), e);
         if (e.getMessage() != null && e.getMessage().toLowerCase().contains("user not found")) {
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error scheduling email."));
    }
}

@PostMapping("/emails/{gmailMessageId}/priority")
public ResponseEntity<?> updatePriorityOnly(
        @PathVariable String gmailMessageId,
        @RequestBody PriorityUpdateRequest request,
        @AuthenticationPrincipal OAuth2User principal) {
    Long internalUserId = getInternalUserId(principal);
    logger.info("API CALL: POST /api/emails/{}/priority for internalUserId: {}, priority: {}", gmailMessageId, internalUserId, request.getPriority());

    try {
        if (request.getPriority() == null || request.getPriority().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Priority value cannot be null or empty."));
        }
        if (request.getSubject() == null || request.getSender() == null) {
             logger.warn("Subject or Sender is null for priority-only update for gmailMessageId: {}. Service method might need to handle this.", gmailMessageId);
             // Depending on service logic, this might be an error or okay if just updating.
        }

        EmailMetadata updatedMetadata = metadataService.setPriority(
                internalUserId,
                gmailMessageId,
                request.getPriority(),
                request.getSubject(), // Still passing, service layer decides if they're needed
                request.getSender()
        );
        return ResponseEntity.ok(Map.of("message", "Priority updated successfully", "data", updatedMetadata));
    } catch (IllegalStateException e) {
        logger.error("API ERROR: Authentication or state issue for userId {}: {}", internalUserId, e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
    } catch (RuntimeException e) {
        logger.error("API ERROR: Setting priority for internalUserId {}, gmailMessageId {}: {}", internalUserId, gmailMessageId, e.getMessage(), e);
        if (e.getMessage() != null && e.getMessage().toLowerCase().contains("user not found")) {
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error setting priority."));
    }
}

@GetMapping("/reminders/upcoming")
public ResponseEntity<?> getUpcomingReminders(@AuthenticationPrincipal OAuth2User principal) {
    Long internalUserId = getInternalUserId(principal);
    logger.info("API CALL: GET /api/reminders/upcoming for internalUserId: {}", internalUserId);
    try {
        List<EmailMetadata> upcomingReminders = metadataService.getUpcomingReminders(internalUserId);
        return ResponseEntity.ok(upcomingReminders);
    } catch (IllegalStateException e) {
        logger.error("API ERROR: Authentication or state issue for userId {}: {}", internalUserId, e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
    } catch (RuntimeException e) {
        logger.error("API ERROR: Fetching upcoming reminders for internalUserId {}: {}", internalUserId, e.getMessage(), e);
         if (e.getMessage() != null && e.getMessage().toLowerCase().contains("user not found")) {
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error fetching upcoming reminders."));
    }
}
}