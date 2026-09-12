package com.web.campus;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Private campus records and inboxes must not survive account changes in shared HTTP caches. */
@Component
public class CampusPrivacyFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if (path.startsWith("/api/campus/") || path.startsWith("/api/notifications")) {
            response.setHeader("Cache-Control", "no-store");
            response.addHeader("Vary", "Authorization");
        }
        chain.doFilter(request, response);
    }
}
