package com.example.minder.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_id", unique = true, nullable = false, length = 255)
    private String googleId;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<EmailMetadata> emailMetadataEntries = new HashSet<>();

    public User() {
    }

    public User(String googleId, String email, String displayName) {
        this.googleId = googleId;
        this.email = email;
        this.displayName = displayName;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Set<EmailMetadata> getEmailMetadataEntries() { return emailMetadataEntries; }
    public void setEmailMetadataEntries(Set<EmailMetadata> emailMetadataEntries) { this.emailMetadataEntries = emailMetadataEntries; }

    public void addEmailMetadata(EmailMetadata metadata) {
        this.emailMetadataEntries.add(metadata);
        metadata.setUser(this);
    }

    public void removeEmailMetadata(EmailMetadata metadata) {
        this.emailMetadataEntries.remove(metadata);
        metadata.setUser(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(googleId, user.googleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(googleId);
    }

    @Override
    public String toString() {
        return "User{" + "id=" + id + ", googleId='" + googleId + '\'' + ", email='" + email + '\'' +
               ", displayName='" + displayName + '\'' + ", emailMetadataEntriesCount=" +
               (emailMetadataEntries != null ? emailMetadataEntries.size() : 0) + '}';
    }
}