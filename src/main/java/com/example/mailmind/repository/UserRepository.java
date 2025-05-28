package com.example.minder.repository; // Corrected package

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.minder.model.User;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleId(String googleId); // No static, no body

    boolean existsByEmail(String email);

    boolean existsByGoogleId(String googleId);

    List<User> findByDisplayNameContainingIgnoreCase(String displayNameFragment);
    long deleteByEmail(String email); // Ensure this is called in a @Transactional service method

    @Query("SELECT u FROM User u WHERE LOWER(u.displayName) LIKE LOWER(concat('%', :searchTerm, '%'))")
    List<User> findUsersByDisplayNameSearch(@Param("searchTerm") String searchTerm);

    // Projection interface
    public interface UserBasicInfo {
        Long getId();
        String getEmail();
        String getDisplayName();
    }

    @Query("SELECT u.id as id, u.email as email, u.displayName as displayName FROM User u")
    List<UserBasicInfo> findAllUserBasicInfo();

    @Query("SELECT DISTINCT u FROM User u JOIN u.emailMetadataEntries e")
    List<User> findUsersWithEmailMetadata();

}