package it.eng.onenet.dsp.api.rest_template.consume_data;

import it.eng.onenet.dsp.api.dto.list_results.ListResultsDataDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class ConsumeDataRestTemplate {

    private final RestTemplate restTemplate;
    
    @Value("${sofia.uri}")
    private String sofiaUri;

    public ConsumeDataRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Map<String, Object>> getList(Map<String, String> headers) {

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Type", "application/json");
        httpHeaders.add("Authorization", headers.get("authorization"));
        HttpEntity httpEntity = new HttpEntity(httpHeaders);

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(this.sofiaUri + "/datalist/data_consumed",
                HttpMethod.GET,
                httpEntity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {
                }
        );

        return response.getBody();
    }

    public ListResultsDataDTO getPage(Map<String, String> headers, Long page) {

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Type", "application/json");
        httpHeaders.add("Authorization", headers.get("authorization"));
        HttpEntity httpEntity = new HttpEntity(httpHeaders);

        ResponseEntity<ListResultsDataDTO> response = restTemplate.exchange(this.sofiaUri + "/datalist/data_consumed/page/" + page,
                HttpMethod.GET,
                httpEntity,
                new ParameterizedTypeReference<ListResultsDataDTO>() {
                }
        );

        return response.getBody();
    }

    public Map<String, Object> getEntity(Map<String, String> headers, String id) {

      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.add("Content-Type", "application/json");
      httpHeaders.add("Authorization", headers.get("authorization"));
      HttpEntity httpEntity = new HttpEntity(httpHeaders);

      ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
          this.sofiaUri + "/dataset/data_consumed/" + id, HttpMethod.GET, httpEntity,
          new ParameterizedTypeReference<Map<String, Object>>() {
          });

      return response.getBody();
    }

}
