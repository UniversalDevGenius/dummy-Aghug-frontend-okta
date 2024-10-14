package com.example.sboktademo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@SpringBootApplication
@EnableWebSecurity
@RestController
public class SboktademoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SboktademoApplication.class, args);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/", "/error").permitAll()
				.anyRequest().authenticated()
			)
			.oauth2Login(oauth2 -> oauth2
				.defaultSuccessUrl("/loginSuccess", true)
				.failureUrl("/loginFailure")
			)
			.exceptionHandling(e -> e
				.accessDeniedPage("/accessDenied")
			);
		return http.build();
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@GetMapping("/user")
	public Map<String, Object> user(@AuthenticationPrincipal OAuth2User principal) {
		return Collections.singletonMap("name", principal.getAttribute("name"));
	}

	@GetMapping("/loginSuccess")
	public String loginSuccess(@AuthenticationPrincipal OAuth2User principal,
							   @RegisteredOAuth2AuthorizedClient("okta") OAuth2AuthorizedClient authorizedClient) {
		String idToken = authorizedClient.getAccessToken().getTokenValue();
		String accessToken = authorizedClient.getAccessToken().getTokenValue();

		boolean authorized = false;
		// Send user principal and tokens to external authorization service
		if (principal != null && idToken != null && accessToken != null) {
			authorized = true;
		}
		
		//sendToExternalAuthService(principal, idToken, accessToken);

		if (authorized) {
			return "redirect:/ "+principal.toString()+" code token "+idToken+" access token  "+accessToken;
		} else {
			return "redirect:/accessDenied";
		}
	}

	private boolean sendToExternalAuthService(OAuth2User principal, String idToken, String accessToken) {
		// Implement the logic to send data to external authorization service
		// This is a placeholder implementation
		RestTemplate restTemplate = restTemplate();
		String externalAuthUrl = "http://external-auth-service.com/authorize";
		
		Map<String, Object> requestBody = Map.of(
			"principal", principal,
			"idToken", idToken,
			"accessToken", accessToken
		);

		try {
			Boolean response = restTemplate.postForObject(externalAuthUrl, requestBody, Boolean.class);
			return response != null && response;
		} catch (Exception e) {
			// Log the error and return false
			e.printStackTrace();
			return false;
		}
	}

	@GetMapping("/loginFailure")
	public String loginFailure() {
		return "Login failed. Please try again.";
	}

	@GetMapping("/accessDenied")
	public String accessDenied() {
		return "Access denied. You don't have permission to access this resource.";
	}

	@GetMapping("/error")
	public String error() {
		return "An error occurred. Please try again later.";
	}
}
