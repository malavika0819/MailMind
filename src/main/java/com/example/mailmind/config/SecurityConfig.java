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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final String frontendDashboardUrl = "/ashboard.html";

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                        "/",               
                        "/index.html",
                        "/login.html",   
                        "/Signup.html",   
                        // CSS files
                        "/index.css",
                        "/login.css",     
                        "/Signup.css",    
                        "/Dashboard.css",
                        // JavaScript files
                        "/script.js",       
                        "/login-script.js", 
                        "/Dashboard.js",   
                        "/reminders.js",    
                        "/Dashboard.html",  
                        "/reminders.html",  
                        "/profile.html",    
                ).permitAll() 
                .requestMatchers("/api/**").authenticated() 
                .anyRequest().authenticated() 
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
