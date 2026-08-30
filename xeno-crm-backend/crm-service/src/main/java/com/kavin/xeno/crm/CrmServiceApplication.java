package com.kavin.xeno.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CrmServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CrmServiceApplication.class, args);
	}

}
