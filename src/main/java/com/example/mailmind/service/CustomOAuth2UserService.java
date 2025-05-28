package com.example.minder.service;

import com.example.minder.model.User; // Your User entity
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    @Autowired
    private UserService appUserService; // Your existing UserService to interact with your DB

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Delegate to the default implementation to fetch the user details from the OAuth2 provider (Google)
        OAuth2User oauth2User = super.loadUser(userRequest);

        // Extract attributes from the OAuth2User. The exact attribute names can vary by provider.
        // For Google, common attributes are 'sub' (subject/ID), 'name', 'given_name', 'family_name', 'email', 'picture'.
        Map<String, Object> attributes = oauth2User.getAttributes();

        String googleId = oauth2User.getName(); // For Google, getName() typically returns the 'sub' (subject) claim, which is a unique ID.
        String email = (String) attributes.get("email");
        String displayName = (String) attributes.get("name"); // Full name
        // String pictureUrl = (String) attributes.get("picture"); // Optional: if you want to store profile picture URL

        if (googleId == null || email == null) {
            logger.error("Could not extract Google ID or email from OAuth2 user attributes: {}", attributes);
            throw new OAuth2AuthenticationException("Missing Google ID or email in OAuth2 user attributes.");
        }

        logger.info("Processing OAuth2 user: GoogleID='{}', Email='{}', DisplayName='{}'", googleId, email, displayName);

        // Use your appUserService to find or create the user in your local database
        User internalUser = appUserService.createOrUpdateUser(googleId, email, displayName);
        logger.info("Local user processed: ID='{}', Email='{}'", internalUser.getId(), internalUser.getEmail());

        // You can add application-specific authorities/roles here if needed
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER")); // Example: all OAuth2 users get ROLE_USER

        // The 'nameAttributeKey' is the key in the 'attributes' map that Spring Security will use
        // to get the principal's "name" (e.g., for @AuthenticationPrincipal OAuth2User principal.getName()).
        // For Google, 'sub' is the standard unique identifier. If getName() returns 'sub', that's fine.
        // Or you can use 'email' if you prefer that as the principal name internally.
        String nameAttributeKey = userRequest.getClientRegistration().getProviderDetails()
                                             .getUserInfoEndpoint().getUserNameAttributeName(); // e.g., "sub" for Google

        // Create a new DefaultOAuth2User (or your custom UserDetails implementation that implements OAuth2User)
        // This DefaultOAuth2User will be available as the Principal in Spring Security context.
        return new DefaultOAuth2User(
                authorities,
                attributes,
                nameAttributeKey // This tells Spring Security which attribute to use as the "name" of the principal
        );
    }
}