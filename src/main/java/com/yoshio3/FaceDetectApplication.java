package com.yoshio3;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.stereotype.Component;

import javax.ws.rs.ApplicationPath;

@SpringBootApplication
@EnableDiscoveryClient
public class FaceDetectApplication {

	public static void main(String[] args) {
		SpringApplication.run(FaceDetectApplication.class, args);
	}

	@Component
	@ApplicationPath("api")
	static class JerseryConfig extends ResourceConfig {
		public JerseryConfig() {
			packages(true, "com.yoshio3");
		}
	}

}
