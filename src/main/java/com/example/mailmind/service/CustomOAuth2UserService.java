package com.example.minder.service;

import com.example.minder.model.User; 
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
        OAuth2User oauth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oauth2User.getAttributes();

        String googleId = oauth2User.getName(); 
        String email = (String) attributes.get("email");
        String displayName = (String) attributes.get("name"); 

        if (googleId == null || email == null) {
            logger.error("Could not extract Google ID or email from OAuth2 user attributes: {}", attributes);
            throw new OAuth2AuthenticationException("Missing Google ID or email in OAuth2 user attributes.");
        }

        logger.info("Processing OAuth2 user: GoogleID='{}', Email='{}', DisplayName='{}'", googleId, email, displayName);
        User internalUser = appUserService.createOrUpdateUser(googleId, email, displayName);
        logger.info("Local user processed: ID='{}', Email='{}'", internalUser.getId(), internalUser.getEmail());
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER")); // Example: all OAuth2 users get ROLE_USER
        String nameAttributeKey = userRequest.getClientRegistration().getProviderDetails()
                                             .getUserInfoEndpoint().getUserNameAttributeName(); 
        return new DefaultOAuth2User(
                authorities,
                attributes,
                nameAttributeKey 
        );
    }
}
