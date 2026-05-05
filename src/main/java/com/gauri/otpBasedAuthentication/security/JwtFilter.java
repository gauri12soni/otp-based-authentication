package com.gauri.otpBasedAuthentication.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gauri.otpBasedAuthentication.entity.Session;
import com.gauri.otpBasedAuthentication.repository.SessionRepository;
import com.gauri.otpBasedAuthentication.service.CustomUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {


    private final JwtUtil jwtUtil;
    private final SessionRepository sessionRepository;
    private final CustomUserDetailsService userDetailsService;

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            UUID sessionId = jwtUtil.extractSessionId(token);
            String tokenPhone = jwtUtil.extractPhone(token);

            Session session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found"));

            if (session.getExpiresAt().isBefore(Instant.now())) {
                throw new RuntimeException("Session expired");
            }

            if (!session.getUser().getPhone().equals(tokenPhone)) {
                throw new RuntimeException("User mismatch");
            }


            if (!session.getIpAddress().equals(request.getRemoteAddr()) ||
                    !session.getUserAgent().equals(request.getHeader("User-Agent")))
                throw new RuntimeException("Device mismatch");

            request.setAttribute("sessionId", sessionId.toString());

            if (session.getLastActiveAt().isBefore(Instant.now().minusSeconds(300))) {
                session.setLastActiveAt(Instant.now());
                sessionRepository.save(session);
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                var userDetails = userDetailsService.loadUserByUsername(tokenPhone);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }


        } catch (ExpiredJwtException e) {
            sendError(response, HttpStatus.UNAUTHORIZED, "Token expired");
            return;

        } catch (Exception e) {
            sendError(response, HttpStatus.UNAUTHORIZED, "Invalid token or session");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse response, HttpStatus status, String message) throws IOException {

        response.setStatus(status.value());
        response.setContentType("application/json");
        objectMapper.writeValue(
                response.getWriter(),
                Map.of("status", status.value(), "message", message)
        );
    }
}