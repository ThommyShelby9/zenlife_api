package com.api.expo.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.api.expo.models.User;
import com.api.expo.repository.UserRepository;

import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Service
public class JwtService {

    // Dans la classe JwtService
    public static final String SECRET = "hRxa7YRn9YQSAIO25SITxr82R6uuJDNgdC0Q/pe8qnE=";

    // Dans la classe JwtService
    private final String secret = SECRET; // Clé secrète pour signer et vérifier le jeton JWT
    private final long expiration = 3 * 24 * 60 * 60 * 1000; // Durée de validité du jeton (en millisecondes)
    @Autowired
    private UserRepository userRepository;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parserBuilder().setSigningKey(this.getKey()).build().parseClaimsJws(token).getBody();
        return claimsResolver.apply(claims);
    }

    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, ((User) userDetails).getEmail()); // Utilisation de l'email comme sujet
    }

    private Key getKey() {
        final byte[] decoder = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(decoder);
    }

    private String createToken(Map<String, Object> claims, String email) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email) // Utiliser l'email comme sujet
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    

    public boolean validateToken(String jwtToken, UserDetails userDetails) {
        final String email = extractUsername(jwtToken); // Si le token contient l'email
        return (email.equals(userDetails.getUsername()) && !isTokenExpired(jwtToken));
    }
    

    public UserDetails loadUserByEmail(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }
    
        User user = optionalUser.get();
    
        // Convertir votre modèle User en un objet UserDetails compatible
        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPassword(),
            new ArrayList<>()
        );
    }
    

}
