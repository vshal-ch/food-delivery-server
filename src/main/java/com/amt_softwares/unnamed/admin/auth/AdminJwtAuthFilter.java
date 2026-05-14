package com.amt_softwares.unnamed.admin.auth;

import com.amt_softwares.unnamed.admin.auth.entity.Admin;
import com.amt_softwares.unnamed.admin.auth.repository.AdminRepository;
import com.amt_softwares.unnamed.admin.auth.service.AdminJwtService;
import com.amt_softwares.unnamed.common.api.ApiErrorWriter;
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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminJwtAuthFilter extends OncePerRequestFilter {

    private final AdminJwtService adminJwtService;
    private final AdminRepository adminRepository;
    private final ApiErrorWriter apiErrorWriter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "/admin/auth/login".equals(request.getServletPath());
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

        try {
            Claims claims = adminJwtService.extractClaims(authHeader.substring(7));
            Admin admin = adminRepository.findById(UUID.fromString(claims.getSubject()))
                    .filter(Admin::isEnabled)
                    .orElseThrow();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            admin,
                            null,
                            List.of(new SimpleGrantedAuthority("admin"))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.error(e.getMessage());
            apiErrorWriter.write(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired admin token",
                    request.getRequestURI()
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
