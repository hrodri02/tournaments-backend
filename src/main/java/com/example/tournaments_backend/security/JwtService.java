package com.example.tournaments_backend.security;

import static com.example.tournaments_backend.security.SecurityConstants.AUTHORIZATION_HEADER;
import static com.example.tournaments_backend.security.SecurityConstants.ACCESS_TOKEN_EXPIRATION_TIME;
import static com.example.tournaments_backend.security.SecurityConstants.REFRESH_TOKEN_EXPIRATION_TIME;
import static com.example.tournaments_backend.security.SecurityConstants.TOKEN_PREFIX;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class JwtService {
   JwtEncoder encoder;
   JwtDecoder decoder;   

   public String createAccessToken(Authentication authentication) {
      Instant now = Instant.now();
      Instant expiry = now.plus(ACCESS_TOKEN_EXPIRATION_TIME, ChronoUnit.SECONDS);

		String scope = authentication.getAuthorities().stream()
         .map(GrantedAuthority::getAuthority)
         .collect(Collectors.joining(" "));
		JwtClaimsSet claims = JwtClaimsSet.builder()
         .issuer("self")
         .issuedAt(now)
         .expiresAt(expiry)
         .subject(authentication.getName())
         .claim("scope", scope)
         .build();
		return this.encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
   }

   public Long getExpirationTime(String compactJws) {
      try {
         Jwt jwt = this.decoder.decode(compactJws);
         Instant expirationInstant = jwt.getExpiresAt();
         return (expirationInstant != null)? expirationInstant.getEpochSecond() : null;
      }
      catch (JwtException ex) {
         log.warn("Invalid Jwt: " + ex.getLocalizedMessage());
         return null;
      }
   }

   public String createRefreshToken(Authentication authentication) {
      Instant now = Instant.now();
      Instant expiry = now.plus(REFRESH_TOKEN_EXPIRATION_TIME, ChronoUnit.SECONDS);

      String scope = authentication.getAuthorities().stream()
         .map(GrantedAuthority::getAuthority)
         .collect(Collectors.joining(" "));
		JwtClaimsSet claims = JwtClaimsSet.builder()
         .issuer("self")
         .issuedAt(now)
         .expiresAt(expiry)
         .subject(authentication.getName())
         .claim("scope", scope)
         .build();
		return this.encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
   }

   public String getUsername(String compactJws) {
      Jwt jwt = this.decoder.decode(compactJws);
      return jwt.getSubject();
   }

   public String resolveToken(HttpServletRequest req) {
      // get jwt from authorization header
      String bearerToken = req.getHeader(AUTHORIZATION_HEADER);

      // if jwt is in header, remove token prefix
      if (bearerToken != null && bearerToken.startsWith(TOKEN_PREFIX)) {
         return bearerToken.substring(7, bearerToken.length());
      }

      return null;
   }

   public boolean isTokenValid(String compactJws) {
      try {
        // This triggers signature verification and expiration checks
        this.decoder.decode(compactJws);
        return true;
    } catch (JwtException e) {
        // This covers signature failure, expiration, and malformed tokens
        return false;
    }
   }
}
