package com.api.expo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.servers.Server;

@SpringBootApplication
@OpenAPIDefinition(
    info = @Info(
        title = "API CreatiSwap",
        version = "3.0.0",
        description = "Documentation de l'API CreatiSwap avec OpenAPI 3.0",
        contact = @Contact(
            name = "Support CreatiSwap",
            email = "support@creatiswap.com",
            url = "https://creatiswap.onrender.com"
        ),
        license = @License(
            name = "Apache 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0.html"
        )
    ),
    servers = {
        @Server(
            url = "http://localhost:8080",
            description = "Serveur local"
        ),
        @Server(
            url = "https://creati-api-java.onrender.com",
            description = "Serveur de production"
        )
    }
)
@ComponentScan(basePackages = {"com.api.expo"})  // Ajoutez cette ligne si nécessaire
public class ExpoApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExpoApplication.class, args);
    }
}