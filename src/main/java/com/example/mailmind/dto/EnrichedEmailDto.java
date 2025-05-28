package com.example.minder.dto;

import java.time.LocalDateTime;

public class EnrichedEmailDto {
    private String id; // Gmail Message ID
    private String subject;
    private String sender;
    private String snippet;
    private String date; // Could be ISO String from Gmail
    private String currentPriority;
    private LocalDateTime reminderDateTime; // Using LocalDateTime if your service layer transforms it
    private String notes;

    public EnrichedEmailDto() {
    }

    public EnrichedEmailDto(String id, String subject, String sender, String snippet, String date,
                            String currentPriority, LocalDateTime reminderDateTime, String notes) {
        this.id = id;
        this.subject = subject;
        this.sender = sender;
        this.snippet = snippet;
        this.date = date;
        this.currentPriority = currentPriority;
        this.reminderDateTime = reminderDateTime;
        this.notes = notes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCurrentPriority() {
        return currentPriority;
    }

    public void setCurrentPriority(String currentPriority) {
        this.currentPriority = currentPriority;
    }

    public LocalDateTime getReminderDateTime() {
        return reminderDateTime;
    }

    public void setReminderDateTime(LocalDateTime reminderDateTime) {
        this.reminderDateTime = reminderDateTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}