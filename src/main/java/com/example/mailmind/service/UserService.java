package com.example.minder.service;
import com.example.minder.model.User;
import com.example.minder.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;

    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findById(Long id) {
        logger.debug("Finding user by ID: {}", id);
        return userRepository.findById(id);
    }

    public Optional<User> findByGoogleId(String googleId) {
        logger.debug("Finding user by Google ID: {}", googleId);
        return userRepository.findByGoogleId(googleId);
    }

    public Optional<User> findByEmail(String email) {
        logger.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    @Transactional
    public User createOrUpdateUser(String googleId, String email, String displayName) {
        if (googleId == null || googleId.trim().isEmpty()) {
            throw new IllegalArgumentException("Google ID cannot be null or empty");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        logger.info("Attempting to create or update user with Google ID: {} and Email: {}", googleId, email);
        Optional<User> existingUserOpt = userRepository.findByGoogleId(googleId);
        User user;
        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            logger.info("User found by Google ID: {}. Updating details if necessary.", googleId);
            boolean needsUpdate = false;
            if (displayName != null && !displayName.equals(user.getDisplayName())) {
                user.setDisplayName(displayName);
                needsUpdate = true;
            }
            if (!email.equals(user.getEmail())) {
                Optional<User> userWithNewEmail = userRepository.findByEmail(email);
                if (userWithNewEmail.isPresent() && !userWithNewEmail.get().getGoogleId().equals(googleId)) {
                    logger.warn("Email {} is already associated with a different Google ID: {}", email, userWithNewEmail.get().getGoogleId());
                    throw new IllegalStateException("Email " + email + " is already in use by another account.");
                }
                user.setEmail(email);
                needsUpdate = true;
            }
            if (needsUpdate) {
                user = userRepository.save(user);
                logger.info("User {} updated.", user.getId());
            }
        } else {
            Optional<User> userByEmailOpt = userRepository.findByEmail(email);
            if (userByEmailOpt.isPresent()) {
                user = userByEmailOpt.get();
                logger.info("User found by email: {}. Linking Google ID: {} if different.", email, googleId);
                if (user.getGoogleId() == null || !user.getGoogleId().equals(googleId)) {
                    user.setGoogleId(googleId);
                }
                if (displayName != null && !displayName.equals(user.getDisplayName())) {
                    user.setDisplayName(displayName);
                }
                user = userRepository.save(user);
                logger.info("Existing user by email linked/updated. User ID: {}", user.getId());
            } else {
                logger.info("Creating new user with Google ID: {} and Email: {}", googleId, email);
                user = new User(googleId, email, displayName);
                user = userRepository.save(user);
                logger.info("New user created with ID: {}", user.getId());
            }
        }
        return user;
    }

    public List<User> findAllUsers() {
        logger.debug("Fetching all users");
        return userRepository.findAll();
    }

    @Transactional
    public void deleteUser(Long id) {
        logger.info("Attempting to delete user with ID: {}", id);
        if (!userRepository.existsById(id)) {
            logger.warn("User with ID: {} not found for deletion.", id);
            throw new RuntimeException("User not found with ID: " + id + " for deletion.");
        }
        userRepository.deleteById(id);
        logger.info("User with ID: {} deleted successfully.", id);
    }
}