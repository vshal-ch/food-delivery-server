package com.amt_softwares.unnamed.auth;

import com.amt_softwares.unnamed.auth.service.JwtService;
import com.amt_softwares.unnamed.common.api.ApiErrorWriter;
import com.amt_softwares.unnamed.user.entity.User;
import com.amt_softwares.unnamed.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final ApiErrorWriter apiErrorWriter;
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // Do not run this filter if the request is going to /auth/ endpoints
        return path.endsWith("send-otp") || path.endsWith("verify-otp");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtService.extractAccessClaims(token);

            String userId = claims.getSubject();

            User user = userRepository.findById(UUID.fromString(userId)).orElseThrow(() -> new UsernameNotFoundException("User not found!"));

            String role = claims.get("role", String.class);
            logger.info("userId: " + userId + ", role: " + role);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            List.of(new SimpleGrantedAuthority(role))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            log.error(e.getMessage());
            apiErrorWriter.write(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired token"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
