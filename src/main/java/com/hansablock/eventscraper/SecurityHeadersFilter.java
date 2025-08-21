package com.hansablock.eventscraper;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;

@Component
public class SecurityHeadersFilter implements Filter {
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletResponse resp = (HttpServletResponse) response;
    resp.setHeader("X-Content-Type-Options", "nosniff");
    resp.setHeader("X-Frame-Options", "SAMEORIGIN");
    resp.setHeader("Referrer-Policy", "no-referrer-when-downgrade");
    // Allow our own CSS/JS and CDN for flatpickr; disallow inline scripts (migrated to external
    // files). Keep style inline for dynamic attributes.
    resp.setHeader(
        "Content-Security-Policy",
        "default-src 'self'; style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com; script-src 'self' https://cdnjs.cloudflare.com; img-src 'self' data: https:; connect-src 'self';");
    chain.doFilter(request, response);
  }
}
