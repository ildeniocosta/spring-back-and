package com.ildenio.curso.config;

import com.ildenio.curso.services.DBService;
import com.ildenio.curso.services.EmailService;
import com.ildenio.curso.services.MockEmailService;
import com.ildenio.curso.services.SmtpEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.text.ParseException;

@Configuration
@Profile("test")
public class TestConfig {
    @Autowired
    private DBService dbService;

    @Bean
    public Boolean intantiateDatabase() throws ParseException {
        dbService.instantiateTestDatabase();

        return true;
    }
    @Bean
    public EmailService emailService(){
        return new MockEmailService();
    }

}
