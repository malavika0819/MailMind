package com.example.minder.dto;
public class ScheduleAndPriorityRequest {

    private String reminderDateTime; 
    private String priority;         
    private String notes;           
    private String subject;
    private String sender;

    public ScheduleAndPriorityRequest() {
    }
    public ScheduleAndPriorityRequest(String reminderDateTime, String priority, String notes, String subject, String sender) {
        this.reminderDateTime = reminderDateTime;
        this.priority = priority;
        this.notes = notes;
        this.subject = subject;
        this.sender = sender;
    }
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
