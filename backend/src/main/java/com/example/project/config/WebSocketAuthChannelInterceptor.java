package com.example.project.config;

import com.example.project.model.User;
import com.example.project.repository.UserRepository;
import com.example.project.service.UserService;
import com.example.project.service.auth.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Objects;
import java.util.regex.*;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern BU_CREATED  = Pattern.compile("^/topic/bu/([a-zA-Z0-9\\-]+)/projects\\.created$");
    private static final Pattern BU_CRITICAL = Pattern.compile("^/topic/bu/([a-zA-Z0-9\\-]+)/projects\\.critical$");
    private static final Pattern BU_UPDATED  = Pattern.compile("^/topic/bu/([a-zA-Z0-9\\-]+)/projects\\.updated$");

    private static final String GLOBAL_CREATED_TOPIC    = "/topic/projects.created";
    private static final String GLOBAL_CRITICAL_TOPIC   = "/topic/projects.critical";
    private static final String GLOBAL_UPDATED_TOPIC    = "/topic/projects.updated";
    private static final String USER_NOTIFICATION_QUEUE = "/user/queue/notifications";

    private final JwtTokenUtil jwtTokenUtil;
    private final UserService userService;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) throw new AccessDeniedException("NO_TOKEN");

            String token = authHeader.substring(7);
            String email, jti;
            try {
                email = jwtTokenUtil.getUsernameFromToken(token);
                jti = jwtTokenUtil.getJtiFromToken(token);
            } catch (Exception e) {
                throw new AccessDeniedException("INVALID_TOKEN");
            }

            UserDetails userDetails = userService.loadUserByUsername(email);
            boolean valid = jwtTokenUtil.validateToken(token, userDetails) && refreshTokenService.isSessionActive(jti);
            if (!valid) throw new AccessDeniedException("INVALID_TOKEN");

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            accessor.setUser(auth);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            Principal principal = accessor.getUser();
            if (!(principal instanceof Authentication auth) || !auth.isAuthenticated()) {
                throw new AccessDeniedException("NOT_AUTHENTICATED");
            }

            String destination = accessor.getDestination();
            if (USER_NOTIFICATION_QUEUE.equals(destination)) return message;

            User user = userRepository.findByEmail(auth.getName());
            if (user == null) throw new AccessDeniedException("USER_NOT_FOUND");

            String role = user.getRole() != null ? user.getRole().getName() : "";
            boolean isAdmin   = "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role);
            boolean isManager = "MANAGER".equalsIgnoreCase(role) || "ROLE_MANAGER".equalsIgnoreCase(role);

            if (GLOBAL_CREATED_TOPIC.equals(destination)
                    || GLOBAL_CRITICAL_TOPIC.equals(destination)
                    || GLOBAL_UPDATED_TOPIC.equals(destination)) {
                if (isAdmin) return message;
                throw new AccessDeniedException("DEST_NOT_ALLOWED");
            }

            Matcher mCreated  = BU_CREATED.matcher(destination == null ? "" : destination);
            Matcher mCritical = BU_CRITICAL.matcher(destination == null ? "" : destination);
            Matcher mUpdated  = BU_UPDATED.matcher(destination == null ? "" : destination);

            Matcher m = mCreated.matches() ? mCreated
                    : mCritical.matches() ? mCritical
                    : mUpdated.matches() ? mUpdated
                    : null;

            if (m == null) throw new AccessDeniedException("DEST_NOT_ALLOWED");

            String topicBuId = m.group(1);
            if (isAdmin) return message;

            String userBu = user.getBusinessUnit() != null ? user.getBusinessUnit().getIdBusinessUnit() : null;
            if (isManager && Objects.equals(userBu, topicBuId)) return message;
            throw new AccessDeniedException("BU_NOT_ALLOWED");
        }

        return message;
    }
}