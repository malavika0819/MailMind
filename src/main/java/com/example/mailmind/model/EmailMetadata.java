package com.example.minder.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "email_metadata",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_user_gmail_message", columnNames = {"user_id", "gmail_message_id"})
       }
)
public class EmailMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_emailmetadata_user"))
    private User user;

    @Column(name = "gmail_message_id", nullable = false, length = 255)
    private String gmailMessageId;

    @Column(length = 50)
    private String priority;

    @Column(name = "reminder_date_time")
    private LocalDateTime reminderDateTime;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(length = 1024)
    private String subject;

    @Column(length = 512)
    private String sender;

    @Column(name = "is_notified", nullable = false)
    private boolean notified = false;

    public EmailMetadata() {
    }

    public EmailMetadata(User user, String gmailMessageId, String subject, String sender) {
        this.user = user;
        this.gmailMessageId = gmailMessageId;
        this.subject = subject;
        this.sender = sender;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getGmailMessageId() { return gmailMessageId; }
    public void setGmailMessageId(String gmailMessageId) { this.gmailMessageId = gmailMessageId; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public LocalDateTime getReminderDateTime() { return reminderDateTime; }
    public void setReminderDateTime(LocalDateTime reminderDateTime) { this.reminderDateTime = reminderDateTime; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public boolean isNotified() { return notified; }
    public void setNotified(boolean notified) { this.notified = notified; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmailMetadata that = (EmailMetadata) o;
        if (this.id != null && that.id != null && Objects.equals(this.id, that.id)) {
            return true;
        }
        if (user == null || user.getId() == null || that.user == null || that.user.getId() == null) {
            return false;
        }
        return Objects.equals(user.getId(), that.user.getId()) &&
               Objects.equals(gmailMessageId, that.gmailMessageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user != null ? user.getId() : null, gmailMessageId);
    }

    @Override
    public String toString() {
        return "EmailMetadata{" + "id=" + id + ", userId=" + (user != null ? user.getId() : "null") +
               ", gmailMessageId='" + gmailMessageId + '\'' + ", priority='" + priority + '\'' +
               ", reminderDateTime=" + reminderDateTime + ", notified=" + notified +
               ", subject='" + (subject != null ? subject.substring(0, Math.min(subject.length(), 50)) + "..." : "null") + '\'' +
               '}';
    }
}