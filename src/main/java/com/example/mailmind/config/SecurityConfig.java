package com.example.minder.config;

import com.example.minder.service.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
// import org.springframework.security.web.csrf.CookieCsrfTokenRepository; // Only if using CSRF with cookies

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    // ** MODIFICATION 1: Corrected the frontendDashboardUrl **
    // This tells Spring Security to redirect to /dashboard.html served by this Spring Boot app.
    private final String frontendDashboardUrl = "/ashboard.html";

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // ** MODIFICATION 2: Refined permitAll() for static assets **
                // These paths are relative to your 'src/main/resources/static/' folder.
                .requestMatchers(
                        "/",                // Your landing page (index.html)
                        "/index.html",
                        "/login.html",    // If you have a separate login page
                        "/Signup.html",   // If you have a separate signup page
                        // CSS files
                        "/index.css",
                        "/login.css",     // If exists
                        "/Signup.css",    // If exists
                        "/Dashboard.css", // For your dashboard
                        // JavaScript files
                        "/script.js",       // For index.html if any
                        "/login-script.js", // If exists
                        "/Dashboard.js",    // For your dashboard
                        "/reminders.js",    // If you have a separate reminders.js
                        // HTML pages (their assets must also be permitted above or by patterns)
                        "/Dashboard.html",  // The dashboard page itself
                        "/reminders.html",  // If you have this
                        "/profile.html",    // If you have this
                        // General patterns
                        "/images/**",       // Allow access to /static/images/
                        "/webjars/**",      // For libraries like Bootstrap if used as webjars
                        "/error",           // Spring Boot's default error page
                        // OAuth2 specific paths
                        "/oauth2/**",
                        "/login/oauth2/code/*" // Callback URI from Google
                ).permitAll() // All above paths are publicly accessible
                .requestMatchers("/api/**").authenticated() // All your API endpoints require authentication
                .anyRequest().authenticated() // Any other request not listed above also requires authentication
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(authenticationSuccessHandler()) // Uses the bean defined below
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login.html?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(e -> e
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    request -> request.getRequestURI().startsWith("/api")
                )
            )
            .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        SimpleUrlAuthenticationSuccessHandler successHandler = new SimpleUrlAuthenticationSuccessHandler();
        successHandler.setDefaultTargetUrl(frontendDashboardUrl); // Will redirect to "/dashboard.html"
        successHandler.setAlwaysUseDefaultTargetUrl(true);
        return successHandler;
    }
}