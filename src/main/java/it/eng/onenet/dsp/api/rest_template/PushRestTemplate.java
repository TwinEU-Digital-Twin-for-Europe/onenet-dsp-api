package it.eng.onenet.dsp.api.rest_template;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

@Service
@Slf4j
public class PushRestTemplate {

  @Value("${push.encryption.key}")
  private String pushEncryptionKey;

  private final RestTemplate restTemplate;

  public PushRestTemplate(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public Integer post(Map<String, Map<String, Object>> parameters, Map<String, String> headers) {

    Map<String, Object> dataSend = parameters != null ? parameters.get("data_send") : null;

    // Retrieve Push_URI security fields
    String uri = dataSend != null && dataSend.get("push_uri") != null ? dataSend.get("push_uri").toString() : null;
    String securityType = dataSend != null && dataSend.get("push_security_type") != null ? dataSend.get("push_security_type").toString() : null;
    String field1 = dataSend != null && dataSend.get("push_security_field1") != null ? dataSend.get("push_security_field1").toString() : null;
    String field2 = dataSend != null && dataSend.get("push_security_field2") != null ? dataSend.get("push_security_field2").toString() : null;
    String addingTo = dataSend != null && dataSend.get("push_security_addingto") != null ? dataSend.get("push_security_addingto").toString() : null;

    // Validate Push_URI
    if (uri == null || uri.isBlank()) {
      log.error("Invalid Push_URI. Aborting request.");
      return 500;
    }

    // Decrypt field2 if present
    try {
      if (field2 != null) {
        field2 = decryptAES(field2);
      }
    } catch (Exception e) {
      log.error("Error decrypting push_security_field2", e);
      return 500;
    }

    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add("Content-Type", "application/json");

    // Handle authentication based on security type
    if ("BASIC".equalsIgnoreCase(securityType)) {
      if (field1 == null || field2 == null) {
        log.error("Missing required security fields. Aborting request.");
        return 500;
      }
      String auth = field1 + ":" + field2;
      String encoded = java.util.Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
      httpHeaders.add("Authorization", "Basic " + encoded);

    } else if ("API-KEY".equalsIgnoreCase(securityType)) {
      if (field1 == null || field2 == null || addingTo == null
          || (!"HEADER".equalsIgnoreCase(addingTo) && !"QUERY-PARAMS".equalsIgnoreCase(addingTo))) {
        log.error("Missing required security fields. Aborting request.");
        return 500;
      }
      if ("HEADER".equalsIgnoreCase(addingTo)) {
        httpHeaders.add(field1, field2);
      } else {
        String separator = uri.contains("?") ? "&" : "?";
        uri = uri + separator + field1 + "=" + field2;
      }

    } else if (!"NO-AUTH".equalsIgnoreCase(securityType)) {
      log.error("Push_URI security type undefined. Aborting request.");
      return 500;
    }

    log.debug("Push_URI Request - URI: {}, Headers: {}",
        uri,
        httpHeaders);

    HttpEntity<Map<String, Map<String, Object>>> httpEntity = new HttpEntity<>(parameters, httpHeaders);

    try {
      ResponseEntity<String> response = restTemplate.exchange(
          URI.create(uri),
          HttpMethod.POST,
          httpEntity,
          new ParameterizedTypeReference<String>() {
          });

      log.debug("Push_URI Response - Code: {}, Body: {}",
          response.getStatusCode().value(),
          response.getBody());

      return response.getStatusCode().value();
    } catch (Exception e) {
      log.error("Error during Push_URI [{}] request", uri, e);
      return 500;
    }
  }

  // Decrypt a Base64-encoded string using AES/ECB/PKCS5Padding
  private String decryptAES(String encrypted) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
    cipher.init(Cipher.DECRYPT_MODE,
        new SecretKeySpec(pushEncryptionKey.getBytes(StandardCharsets.UTF_8), "AES"));
    byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encrypted));
    String decrypted = new String(decryptedBytes, StandardCharsets.UTF_8);
    return decrypted;
  }

}
