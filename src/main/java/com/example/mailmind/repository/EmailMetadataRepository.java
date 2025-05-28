package com.example.minder.repository;

import com.example.minder.model.EmailMetadata;
import com.example.minder.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailMetadataRepository extends JpaRepository<EmailMetadata, Long> {

    // Basic finders
    List<EmailMetadata> findByUser(User user);
    List<EmailMetadata> findByUserId(Long userId);
    Optional<EmailMetadata> findByUserAndGmailMessageId(User user, String gmailMessageId);
    Optional<EmailMetadata> findByUserIdAndGmailMessageId(Long userId, String gmailMessageId);

    // Finders by priority
    List<EmailMetadata> findByUserAndPriority(User user, String priority);
    List<EmailMetadata> findByUserIdAndPriority(Long userId, String priority);

    // Finders related to reminders
    List<EmailMetadata> findByReminderDateTimeIsNotNull();
    List<EmailMetadata> findByUserAndReminderDateTimeIsNotNull(User user);
    List<EmailMetadata> findByUserIdAndReminderDateTimeIsNotNull(Long userId);
    List<EmailMetadata> findByReminderDateTimeBefore(LocalDateTime dateTime);
    List<EmailMetadata> findByUserAndReminderDateTimeBetween(User user, LocalDateTime startDateTime, LocalDateTime endDateTime);
    @Query(value = "SELECT * FROM email_metadata em WHERE em.user_id = :#{#user.id} AND LOWER(em.notes) LIKE :processedSearchTerm", nativeQuery = true)
    List<EmailMetadata> findByUserAndNotesLikeProcessed(@Param("user") User user, @Param("processedSearchTerm") String processedSearchTerm);

    @Query(value = "SELECT * FROM email_metadata em WHERE LOWER(em.notes) LIKE :processedSearchTerm", nativeQuery = true)
    List<EmailMetadata> findByNotesLikeProcessed(@Param("processedSearchTerm") String processedSearchTerm);

    // Count methods
    long countByUser(User user);
    long countByUserId(Long userId);
    long countByUserAndPriority(User user, String priority);

    // Modifying queries
    @Modifying
    @Query("UPDATE EmailMetadata em SET em.priority = :newPriority WHERE em.user.id = :userId")
    int updateUserPriorityByUserId(@Param("userId") Long userId, @Param("newPriority") String newPriority);

    @Modifying
    @Query("DELETE FROM EmailMetadata em WHERE em.user = :user AND (em.priority = 'none' OR em.priority IS NULL OR em.priority = '')")
    int deleteUserMetadataWithNoOrNullPriority(@Param("user") User user);

    // Method for NotificationSchedulerService
    @Query("SELECT em FROM EmailMetadata em WHERE em.reminderDateTime IS NOT NULL AND em.notified = false AND em.reminderDateTime <= :currentTime")
    List<EmailMetadata> findDueAndUnnotifiedReminders(@Param("currentTime") LocalDateTime currentTime);
    public interface EmailReminderDetails {
        String getGmailMessageId();
        LocalDateTime getReminderDateTime();
        String getSubject();
        String getNotes();
    }
    @Query("SELECT em.gmailMessageId as gmailMessageId, " +
           "em.reminderDateTime as reminderDateTime, " +
           "em.subject as subject, " +
           "em.notes as notes " +
           "FROM EmailMetadata em " +
           "WHERE em.user.id = :userId AND em.reminderDateTime IS NOT NULL AND em.reminderDateTime > :now " +
           "ORDER BY em.reminderDateTime ASC")
    List<EmailReminderDetails> findUpcomingReminderDetailsForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
