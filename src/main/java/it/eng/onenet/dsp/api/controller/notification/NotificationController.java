package it.eng.onenet.dsp.api.controller.notification;

import java.text.MessageFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import it.eng.onenet.dsp.api.dto.notification.FirebaseTokenResponseDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/notifications")
@Slf4j
@Tag(name = "Notification", description = "Endpoints for notification module integration")
public class NotificationController {

	@Value("${notification.module.endpoint}")
	private String notificationModuleEndpoint;

	private final RestTemplate restTemplate;

	public NotificationController(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@PostMapping(value = "/auth/firebase-token", produces = MediaType.APPLICATION_JSON_VALUE)
	public FirebaseTokenResponseDTO getFirebaseToken(@RequestHeader HttpHeaders headers) {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add("Content-Type", "application/json");
		headers.forEach((key, values) -> {
			if ("authorization".equalsIgnoreCase(key)) {
				httpHeaders.add("Authorization", values.get(0));
			}
		});

		HttpEntity<?> httpEntity = new HttpEntity<>(httpHeaders);
		ResponseEntity<FirebaseTokenResponseDTO> response = restTemplate.postForEntity(
				MessageFormat.format("{0}/auth/firebase-token", this.notificationModuleEndpoint),
				httpEntity,
				FirebaseTokenResponseDTO.class);

		return response.getBody();
	}
}
