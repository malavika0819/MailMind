package com.example.minder.dto;

// No external imports needed for this simple DTO

public class ScheduleAndPriorityRequest {

    private String reminderDateTime; // Expecting ISO String e.g., "YYYY-MM-DDTHH:mm:ss"
    private String priority;         // e.g., "high", "medium", "low", "none"
    private String notes;            // Optional
    private String subject;
    private String sender;

    // Default constructor
    public ScheduleAndPriorityRequest() {
    }

    // Constructor with all fields (optional)
    public ScheduleAndPriorityRequest(String reminderDateTime, String priority, String notes, String subject, String sender) {
        this.reminderDateTime = reminderDateTime;
        this.priority = priority;
        this.notes = notes;
        this.subject = subject;
        this.sender = sender;
    }

    // Getters
    public String getReminderDateTime() {
        return reminderDateTime;
    }

    public String getPriority() {
        return priority;
    }

    public String getNotes() {
        return notes;
    }

    public String getSubject() {
        return subject;
    }

    public String getSender() {
        return sender;
    }

    // Setters
    public void setReminderDateTime(String reminderDateTime) {
        this.reminderDateTime = reminderDateTime;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    @Override
    public String toString() {
        return "ScheduleAndPriorityRequest{" +
                "reminderDateTime='" + reminderDateTime + '\'' +
                ", priority='" + priority + '\'' +
                ", notes='" + (notes != null ? notes.substring(0, Math.min(notes.length(), 30)) + "..." : "null") + '\'' +
                ", subject='" + subject + '\'' +
                ", sender='" + sender + '\'' +
                '}';
    }
}