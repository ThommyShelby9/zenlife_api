package com.api.expo.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    // Liste des chemins publics à ignorer
    private List<String> publicPaths = Arrays.asList(
        "/api/login",
        "/api/register",
        "/api/verify-email",
        "/api/password/reset-request",
        "/api/password/reset",
        "/api/verification/resend",
        "/api/",
        "/api/contact",
        "/v3/api-docs",
        "/swagger-ui",
        "/ws",
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/verify-email",
        "/api/auth/**"

            );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Vérifier si le chemin fait partie des chemins publics
        String path = request.getRequestURI();
        
        // Ajouter des logs pour le débogage
        System.out.println("Path demandé: " + path);
        
        // Vérification plus précise des chemins publics
        boolean isPublicPath = false;
        for (String publicPath : publicPaths) {
            if (path.equals(publicPath) || path.startsWith(publicPath + "/")) {
                isPublicPath = true;
                break;
            }
        }
        
        System.out.println("Est un chemin public: " + isPublicPath);
        
        // Si c'est un chemin public, passer directement au filtre suivant
        if (isPublicPath) {
            filterChain.doFilter(request, response);
            return;
        }
        
        final String authorizationHeader = request.getHeader("Authorization");
        
        // Ajouter des logs pour le débogage des en-têtes
        if (authorizationHeader != null) {
            System.out.println("En-tête Authorization trouvé: " + 
                              (authorizationHeader.length() > 20 ? 
                               authorizationHeader.substring(0, 20) + "..." : 
                               authorizationHeader));
        } else {
            System.out.println("Aucun en-tête Authorization trouvé");
        }
    
        String email = null;
        String jwtToken = null;
    
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwtToken = authorizationHeader.substring(7);
            try {
                email = jwtService.extractUsername(jwtToken);
                System.out.println("Email extrait du token: " + email);
            } catch (Exception e) {
                System.out.println("Erreur lors de l'extraction du token: " + e.getMessage());
            }
        }
    
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = jwtService.loadUserByEmail(email);
                System.out.println("UserDetails chargé pour: " + userDetails.getUsername());
                
                if (jwtService.validateToken(jwtToken, userDetails)) {
                    System.out.println("Token JWT validé avec succès");
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                } else {
                    System.out.println("Échec de validation du token JWT");
                }
            } catch (Exception e) {
                System.out.println("Erreur lors du chargement des détails utilisateur: " + e.getMessage());
            }
        }
    
        filterChain.doFilter(request, response);
    }
}