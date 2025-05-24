package com.abada.engine.api.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TrailingSlashFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getRequestURI();
        if (path.endsWith("/") && !path.equals("/")) {
            String newPath = path.replaceAll("/+$", "");
            ((HttpServletResponse) response).sendRedirect(newPath);
            return;
        }
        chain.doFilter(request, response);
    }
}