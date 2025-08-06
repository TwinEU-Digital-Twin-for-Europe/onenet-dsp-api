package it.eng.onenet.dsp.api.rest_template;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

@Service
@Slf4j
public class PushRestTemplate {

    private final RestTemplate restTemplate;

    public PushRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Integer post(Map < String, Map < String, Object >> parameters,        
        Map < String, String > headers) {        
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Type", "application/json");
        // httpHeaders.add("Authorization", headers.get("authorization"));

        log.debug("headers " + httpHeaders.toString());
        HttpEntity < Map < String, Map < String, Object >>> httpEntity = new HttpEntity < > (parameters, httpHeaders);
        String uri=(String) parameters.get("data_send").get("push_uri");

        log.debug("PUSH_URI to Request: " + uri);


        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    URI.create(uri), HttpMethod.POST, httpEntity, new ParameterizedTypeReference<String>()  
        {});
            log.debug("Response Code Push_uri request: " + response.getStatusCodeValue());
            log.debug("Response Body Push_uri request: " + response.getBody().toString());            
            return response.getStatusCodeValue();
        } catch (Exception e) {
            log.error("Error during PUSH_URI request " + uri, e);
            return 500;  
        }
    }

}



