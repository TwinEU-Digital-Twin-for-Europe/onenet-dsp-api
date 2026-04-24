package it.eng.onenet.dsp.api.service.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import it.eng.onenet.dsp.api.dto.notification.DataProvidedEventDTO;
import it.eng.onenet.dsp.api.dto.notification.NotificationDTO;
import it.eng.onenet.dsp.api.dto.notification.NotificationResponseDTO;
import it.eng.onenet.dsp.api.dto.notification.NotificationType;
import it.eng.onenet.dsp.api.dto.notification.NotificationUserDTO;
import it.eng.onenet.dsp.api.dto.request.DataRequestDTO;
import it.eng.onenet.dsp.api.dto.user.UserDTO;
import it.eng.onenet.dsp.api.dto.user.UserFullDTO;
import it.eng.onenet.dsp.api.rest_template.user.UserRestTemplate;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationService {

	@Value("${sofia.uri}")
	private String sofiaUri;

	@Value("${connector.enable.notifications}")
	private Boolean connectorEnableNotifications;

	@Value("${notification.module.endpoint}")
	private String notificationModuleEndpoint;

	private final RestTemplate restTemplate;
	private final UserRestTemplate userRestTemplate;

	public NotificationService(RestTemplate restTemplate, UserRestTemplate userRestTemplate) {
		this.restTemplate = restTemplate;
		this.userRestTemplate = userRestTemplate;
	}

	@Async("taskExecutor")
	public void DataProvided(Map<String, String> headers, DataProvidedEventDTO event) {
		try {
			if (this.connectorEnableNotifications) {
				log.info(
						"Start Notification Process for Data Entity Provided [{}] - Data Offering [{}] - type [{}]",
						event.getDataEntityId(), event.getDataOfferingId(), event.getDataOfferingType());

				HttpHeaders httpHeaders = new HttpHeaders();
				httpHeaders.add("Content-Type", "application/json");
				httpHeaders.add("Authorization", headers.get("authorization"));
				HttpEntity<?> httpEntity = new HttpEntity<>(httpHeaders);

				// ******** service type = PUSH */
				if ("push".equalsIgnoreCase(event.getDataOfferingType())) {

					// *** fromUser ***
					NotificationUserDTO fromUser = getFromUserPush(headers, httpEntity);
					// *** fromUser ***

					// *** toUser ***
					NotificationUserDTO toUser = getToUserPush(event, httpEntity);
					// *** toUser ***

					Map<String, Object> payload = getPayload(event, "push");
					NotificationDTO notification = getNotificationDto(event, fromUser, toUser, payload);

					sendNotification(event, httpHeaders, notification);
					return;
				}

				// ******* service type = Data */
				ResponseEntity<List<DataRequestDTO>> response = restTemplate.exchange(
						MessageFormat.format("{0}/datalist/accepted_requests_on_offered_service?data_offering_id={1}",
								this.sofiaUri, event.getDataOfferingId()),
						HttpMethod.GET, httpEntity, new ParameterizedTypeReference<List<DataRequestDTO>>() {
						});

				List<DataRequestDTO> acceptedRequests = response.getBody();
				if (acceptedRequests == null || acceptedRequests.isEmpty()) {
					log.info("No accepted requests found for data offering [{}]", event.getDataOfferingId());
					return;
				}

				for (DataRequestDTO acceptedRequest : acceptedRequests) {
					log.info("Accepted request [{}] for offering [{}], user requesting [{}]",
							acceptedRequest.getRequestId(), event.getDataOfferingId(), acceptedRequest.getUserRequesting());

					NotificationUserDTO fromUser = new NotificationUserDTO();
					fromUser
							.setId(acceptedRequest.getUserOfferingId() != null ? acceptedRequest.getUserOfferingId().toString() : "");
					fromUser.setDescription(acceptedRequest.getUserOffering());

					NotificationUserDTO toUser = new NotificationUserDTO();
					toUser.setId(
							acceptedRequest.getUserRequestingId() != null ? acceptedRequest.getUserRequestingId().toString() : "");
					toUser.setDescription(acceptedRequest.getUserRequesting());

					Map<String, Object> payload = getPayload(event, "data");
					payload.put("requestId",
							acceptedRequest.getRequestId() != null ? acceptedRequest.getRequestId().toString() : "");

					NotificationDTO notification = getNotificationDto(event, fromUser, toUser, payload);

					sendNotification(event, httpHeaders, notification);
				}
			}
		} catch (

		Exception e) {
			log.error("Error during notification process Data Entity Provided [{0}] - Data Offering [{1}]",
					event.getDataEntityId(), event.getDataOfferingId(), e);
		}
	}

	private NotificationUserDTO getToUserPush(DataProvidedEventDTO event, HttpEntity<?> httpEntity)
			throws JsonProcessingException, JsonMappingException {
		String offerResponse = restTemplate.exchange(
				MessageFormat.format("{0}/dataset/my_offered_services/{1}", this.sofiaUri,
						(event.getDataOfferingId() != null ? event.getDataOfferingId() : "")),
				HttpMethod.GET,
				httpEntity,
				new ParameterizedTypeReference<String>() {
				}).getBody();

		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = (offerResponse != null) ? mapper.readTree(offerResponse) : mapper.createObjectNode();
		JsonNode userObj = root.path("data_catalog_data_offerings_obj").path("user_obj");
		String offerUsernameId = safeGet(() -> userObj.path("id").asText());
		String offerUsername = safeGet(() -> userObj.path("username").asText());
		String offerCompanyName = safeGet(() -> userObj.path("company_obj").path("name").asText());

		String offerDescription;
		if (!offerCompanyName.isBlank() && !offerUsername.isBlank()) {
			offerDescription = offerCompanyName + " - " + offerUsername;
		} else if (!offerCompanyName.isBlank()) {
			offerDescription = offerCompanyName;
		} else if (!offerUsername.isBlank()) {
			offerDescription = offerUsername;
		} else {
			offerDescription = "Unknown User";
		}

		NotificationUserDTO toUser = new NotificationUserDTO(offerUsernameId, offerDescription);
		return toUser;
	}

	private NotificationUserDTO getFromUserPush(Map<String, String> headers, HttpEntity<?> httpEntity) {
		UserDTO user = this.userRestTemplate.getCurrentUser(headers);
		log.info("Get User Details [{}]", user.getId());

		UserFullDTO userFull = restTemplate.exchange(
				MessageFormat.format("{0}/dataset/user/{1}", this.sofiaUri, user.getId()),
				HttpMethod.GET, httpEntity, new ParameterizedTypeReference<UserFullDTO>() {
				}).getBody();

		String username = safeGet(() -> userFull.getUser().getUsername());
		String companyName = safeGet(() -> userFull.getUser().getCompany().getName());

		String description;
		if (!companyName.isBlank() && !username.isBlank()) {
			description = companyName + " - " + username;
		} else if (!companyName.isBlank()) {
			description = companyName;
		} else if (!username.isBlank()) {
			description = username;
		} else {
			description = "Unknown User";
		}

		NotificationUserDTO fromUser = new NotificationUserDTO(user.getId(), description);
		return fromUser;
	}

	private Map<String, Object> getPayload(DataProvidedEventDTO event, String type) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("serviceType", type);
		payload.put("dataEntityId", event.getDataEntityId());
		payload.put("dataEntityTitle", event.getDataEntityTitle());
		payload.put("dataOfferingId", event.getDataOfferingId());
		return payload;
	}

	private void sendNotification(DataProvidedEventDTO event, HttpHeaders httpHeaders,
			NotificationDTO notification) {
		HttpEntity<NotificationDTO> notificationHttpEntity = new HttpEntity<>(notification, httpHeaders);
		ResponseEntity<NotificationResponseDTO> notificationResponse = restTemplate.postForEntity(
				MessageFormat.format("{0}/notifications", this.notificationModuleEndpoint),
				notificationHttpEntity, NotificationResponseDTO.class);

		if (notificationResponse.getBody() != null) {
			log.info("Notification [{}] sent to user [{}] for data offering [{}, type: {}] from user [{}]",
					notificationResponse.getBody().getNotificationId(), notification.getToUser().getDescription(),
					event.getDataOfferingId(), event.getDataOfferingType(), notification.getFromUser().getDescription());
		}
	}

	private NotificationDTO getNotificationDto(DataProvidedEventDTO event, NotificationUserDTO fromUser,
			NotificationUserDTO toUser, Map<String, Object> payload) {
		NotificationDTO notification = new NotificationDTO();
		notification.setFromUser(fromUser);
		notification.setToUser(toUser);
		notification.setType(NotificationType.DATA_PROVIDED);
		notification.setTitle("Data Provided");
		notification.setBody(String.format("Data entity %s has been provided", event.getDataEntityTitle()));
		notification.setPayload(payload);
		return notification;
	}

	private static String safeGet(Supplier<String> getter) {
		try {
			return Optional.ofNullable(getter.get()).orElse("");
		} catch (Exception e) {
			return "";
		}
	}
}
