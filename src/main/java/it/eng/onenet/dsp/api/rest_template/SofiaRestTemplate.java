package it.eng.onenet.dsp.api.rest_template;

import java.net.URI;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SofiaRestTemplate {

    private final RestTemplate restTemplate;
    @Value("${sofia.uri}")
    private String sofiaUri;

    public SofiaRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String post(Map<String, Map<String, Object>> parameters,
                       String formId,
                       Map<String, String> headers) {

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Type", "application/json");
        httpHeaders.add("Authorization", headers.get("authorization"));

        log.debug("headers " + httpHeaders.toString());
        HttpEntity<Map<String, Map<String, Object>>> httpEntity =
                new HttpEntity<Map<String, Map<String, Object>>>(parameters, httpHeaders);

        ResponseEntity<String> response =
                restTemplate.exchange(
                        URI.create(sofiaUri + "/form?id=" + formId),
                        HttpMethod.POST,
                        httpEntity,
                        new ParameterizedTypeReference<String>() {
                        }
                );

        log.debug("response body " + response.getBody().toString());
        
        return getIdFromResponse(response.getBody());
    }

    /**
     * Validates the input string as JSON. If valid, returns the id field;
     * otherwise, returns the original string
     * 
     * @param responseString string input
     * @return String id
     * @throws JSONException
     */
    public String getIdFromResponse(String responseString) throws JSONException {
        try {
            JSONObject jsonObject = new JSONObject(responseString);
            return jsonObject.getString("id");
        } catch (JSONException ex) {
            try {
                JSONArray jsonArray = new JSONArray(responseString);
                return jsonArray.getJSONObject(0).getString("id");
            } catch (JSONException ex1) {
                return responseString;
            }
        }
    }

}
