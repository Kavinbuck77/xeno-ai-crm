package com.kavin.xeno.channel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ChannelServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChannelServiceApplication.class, args);
	}

}
