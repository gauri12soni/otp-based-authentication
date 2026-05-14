package com.gauri.otpBasedAuthentication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OtpBasedAuthenticationApplication {

	public static void main(String[] args) {
		SpringApplication.run(OtpBasedAuthenticationApplication.class, args);
	}

}
