package com.IngSoftwarelll.mercadox;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvCheck {

    @Bean
    CommandLineRunner printEnv() {
        return args -> {
            System.out.println("DB_URL: " + System.getenv("DB_URL"));
            System.out.println("DB_USERNAME: " + System.getenv("DB_USERNAME"));
            System.out.println("PORT: " + System.getenv("PORT"));
            System.out.println("SENDGRID_API_KEY: " + System.getenv("SENDGRID_API_KEY"));
        };
    }
}