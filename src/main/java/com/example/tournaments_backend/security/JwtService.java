package com.example.tournaments_backend.security;

import static com.example.tournaments_backend.security.SecurityConstants.AUTHORIZATION_HEADER;
import static com.example.tournaments_backend.security.SecurityConstants.EXPIRATION_TIME;
import static com.example.tournaments_backend.security.SecurityConstants.SECRET;
import static com.example.tournaments_backend.security.SecurityConstants.TOKEN_PREFIX;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.example.tournaments_backend.app_user.AppUserRole;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class JwtService {
    public String createToken(String username, AppUserRole role) {
      Date now = new Date();
      Date validity = new Date(now.getTime() + EXPIRATION_TIME);
      return Jwts.builder()
         .subject(username)
         .claim("auth", role.name())
         .issuedAt(now)
         .expiration(validity)
         .signWith(getSignInKey())
         .compact();
   }

   private SecretKey getSignInKey() {
      byte[] keyBytes = Decoders.BASE64.decode(SECRET);
      return Keys.hmacShaKeyFor(keyBytes);
   }

   public String getUsername(String compactJws) {
      return Jwts
               .parser()
               .verifyWith(getSignInKey())
               .build()
               .parseSignedClaims(compactJws)
               .getPayload()
               .getSubject();
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
         Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(compactJws);;
         return true;
      } catch (JwtException ex) {
         System.out.println(ex.getMessage());
         return false;
      }
   }
    
}
