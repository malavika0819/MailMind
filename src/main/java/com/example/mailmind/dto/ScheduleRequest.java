package com.example.minder.dto;

public class ScheduleRequest {
    private String reminderDateTime; // Expecting ISO String e.g., "2024-12-31T10:00:00"
    private String notes;
    private String subject; // Needed if creating new metadata
    private String sender;  // Needed if creating new metadata

    // Getters and Setters
    public String getReminderDateTime() {
        return reminderDateTime;
    }

    public void setReminderDateTime(String reminderDateTimeISO) {
        this.reminderDateTime = reminderDateTimeISO;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
}