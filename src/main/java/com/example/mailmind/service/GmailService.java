package com.example.minder.service;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class GmailService {
private static final Logger logger = LoggerFactory.getLogger(GmailService.class);
private static final String APPLICATION_NAME = "MailMinder";
private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
private static final String USER_IDENTIFIER = "me";

public List<Map<String, Object>> fetchImportantEmails(Long userId, String accessToken)
        throws IOException, GeneralSecurityException {

    logger.info("Fetching important emails for userId: {} using real Gmail API.", userId);

    if (accessToken == null || accessToken.trim().isEmpty()) {
        logger.error("Access token is null or empty for userId: {}. Cannot fetch emails.", userId);
        return Collections.emptyList();
    }
    logger.debug("Using Access Token for userId: {} (Token starts with: {})", userId,
            accessToken.length() > 10 ? accessToken.substring(0, 10) + "..." : accessToken);

    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod())
                                .setAccessToken(accessToken);

    Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
            .setApplicationName(APPLICATION_NAME)
            .build();

    List<Map<String, Object>> fetchedEmails = new ArrayList<>();

    try {
        String query = "is:important in:inbox"; // Example: Fetch important unread emails from inbox
        long maxResults = 15L; // Fetch up to 15 emails

        logger.debug("Gmail API Query for userId {}: '{}', Max Results: {}", userId, query, maxResults);

        ListMessagesResponse listResponse = service.users().messages().list(USER_IDENTIFIER)
                .setQ(query)
                .setMaxResults(maxResults)
                .execute();

        List<Message> messages = listResponse.getMessages();

        if (messages == null || messages.isEmpty()) {
            logger.info("No messages found matching the query for userId: {}", userId);
            return Collections.emptyList();
        }
        logger.info("Found {} messages for userId: {}. Fetching details...", messages.size(), userId);

        for (Message message : messages) {
            Message fullMessage = service.users().messages().get(USER_IDENTIFIER, message.getId())
                    .setFormat("metadata")
                    .setFields("id,snippet,internalDate,payload/headers")
                    .execute();

            Map<String, Object> emailMap = parseGmailMessage(fullMessage);
            if (emailMap != null) {
                fetchedEmails.add(emailMap);
            }
        }
    } catch (IOException e) {
        logger.error("IOException while fetching emails for userId {}: {}", userId, e.getMessage(), e);
        throw e;
    } catch (Exception e) {
        logger.error("Unexpected error while fetching emails for userId {}: {}", userId, e.getMessage(), e);
        throw new IOException("Failed to retrieve emails from Gmail due to an unexpected error: " + e.getMessage(), e);
    }

    logger.info("Successfully fetched {} emails from Gmail for userId: {}", fetchedEmails.size(), userId);
    return fetchedEmails;
}

private Map<String, Object> parseGmailMessage(Message gmailMessage) {
    if (gmailMessage == null) {
        return null;
    }

    Map<String, Object> emailMap = new HashMap<>();
    emailMap.put("id", gmailMessage.getId());
    emailMap.put("snippet", gmailMessage.getSnippet() != null ? gmailMessage.getSnippet() : "");

    if (gmailMessage.getInternalDate() != null) {
        try {
            Instant instant = Instant.ofEpochMilli(gmailMessage.getInternalDate());
            emailMap.put("date", instant.toString()); // ISO 8601 format (e.g., "2025-05-24T12:30:00Z")
        } catch (Exception e) {
            logger.warn("Could not parse internalDate: {} for messageId: {}", gmailMessage.getInternalDate(), gmailMessage.getId(), e);
            emailMap.put("date", "");
        }
    } else {
        emailMap.put("date", "");
    }

    String subject = "(No Subject)";
    String sender = "(Unknown Sender)";

    if (gmailMessage.getPayload() != null && gmailMessage.getPayload().getHeaders() != null) {
        for (MessagePartHeader header : gmailMessage.getPayload().getHeaders()) {
            String headerName = header.getName();
            String headerValue = header.getValue();
            if (headerValue != null) {
                if ("Subject".equalsIgnoreCase(headerName)) {
                    subject = headerValue;
                } else if ("From".equalsIgnoreCase(headerName)) {
                    sender = headerValue;
                }
            }
        }
    }
    emailMap.put("subject", subject);
    emailMap.put("sender", sender);

    return emailMap;
}
}
