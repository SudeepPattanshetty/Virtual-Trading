package com.vtrade;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    // We use JWT only, so disable Spring's default generated-password user.
    @Bean
    UserDetailsService userDetailsService() {
        return u -> { throw new UsernameNotFoundException(u); };
    }

    @Bean
    SecurityFilterChain chain(HttpSecurity http, JwtService jwt) throws Exception {
        http.csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll())
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterBefore(new JwtFilter(jwt), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Component
    public static class JwtService {
        private final SecretKey key;

        public JwtService(@Value("${app.jwt.secret}") String secret) {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }

        public String create(String username) {
            long now = System.currentTimeMillis();
            return Jwts.builder().subject(username)
                    .issuedAt(new Date(now))
                    .expiration(new Date(now + 7L * 24 * 3600 * 1000))
                    .signWith(key).compact();
        }

        public String username(String token) {
            return Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload().getSubject();
        }
    }

    static class JwtFilter extends OncePerRequestFilter {
        private final JwtService jwt;
        JwtFilter(JwtService jwt) { this.jwt = jwt; }

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
            String h = req.getHeader("Authorization");
            if (h != null && h.startsWith("Bearer ")) {
                try {
                    String user = jwt.username(h.substring(7));
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(user, null, List.of()));
                } catch (Exception ignored) { /* invalid token -> stays unauthenticated */ }
            }
            chain.doFilter(req, res);
        }
    }
}
