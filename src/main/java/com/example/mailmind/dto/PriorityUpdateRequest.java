package com.example.minder.dto;

public class PriorityUpdateRequest {
    private String priority;
    private String subject;
    private String sender;

    // Default constructor
    public PriorityUpdateRequest() {
    }

    // Getters
    public String getPriority() {
        return priority;
    }

    public String getSubject() {
        return subject;
    }

    public String getSender() {
        return sender;
    }

    // Setters
    public void setPriority(String priority) {
        this.priority = priority;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }
}