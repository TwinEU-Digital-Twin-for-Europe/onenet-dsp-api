package it.eng.onenet.dsp.api.service.provide_data;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import it.eng.onenet.dsp.api.dto.FormResponse;
import it.eng.onenet.dsp.api.dto.connector.Dataset;
import it.eng.onenet.dsp.api.dto.connector.Distribution;
import it.eng.onenet.dsp.api.dto.connector.Offer;
import it.eng.onenet.dsp.api.dto.list_results.ListResultsDataDTO;
import it.eng.onenet.dsp.api.dto.provide_data.ProvideDataDTO;
import it.eng.onenet.dsp.api.rest_template.provide_data.ProvideDataRestTemplate;
import it.eng.onenet.dsp.api.service.connector.ConnectorApiService;
import it.eng.onenet.dsp.api.service.local_functionality.EntityService;
import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class ProvideDataService {

  private final ProvideDataRestTemplate provideDataRestTemplate;
  private final EntityService entityService;
  private final ConnectorApiService connectorApiService;

  public ProvideDataService(ProvideDataRestTemplate provideDataRestTemplate, EntityService entityService, ConnectorApiService connectorApiService) {
    this.provideDataRestTemplate = provideDataRestTemplate;
    this.entityService = entityService;
    this.connectorApiService = connectorApiService;
  }

  public List<Map<String, Object>> getList(Map<String, String> headers) {
    return this.provideDataRestTemplate.getList(headers);
  }

  public ListResultsDataDTO getPage(Map<String, String> headers, Long page) {
    return this.provideDataRestTemplate.getPage(headers, page);
  }

  public FormResponse post(ProvideDataDTO dto, Map<String, String> headers) throws IOException {

    Dataset dataset = null;
    String file = null;
    String id = null;
    Integer pushServiceResCode = 0;

    Map<String, Object> dataSend = new HashMap<>();
    dataSend.put("title", dto.getTitle());
    dataSend.put("description", dto.getDescription());
    dataSend.put("fileName", dto.getFilename());
    dataSend.put("fileSize", dto.getFileSize());
    dataSend.put("user_code", dto.getCode());
    dataSend.put("data_catalog_data_offerings_id", dto.getData_offering_id());
    dataSend.put("status", "active");
    Map<String, Map<String, Object>> parameters = new HashMap<>();
    parameters.put("data_send", dataSend);

    Map<String, Object> keywords = new HashMap<>();
    parameters.put("keywords", keywords);

    /* Check on Central Registry if user has rights on this data offering and get push_uri for PUSH service type */
    entityService.checkAndSetDataOfferingInfo(parameters, headers);

    // Collect keywords
    List<String> keywordsList = parameters.get("keywords").values().stream()
        .filter(Objects::nonNull)
        .map(Object::toString)
        .collect(Collectors.toList());

    /* Save File to local True Connector */
    String prefix = "base64,";
    int index = dto.getFile().indexOf(prefix);
    if (index >= 0) file = dto.getFile().substring(index + prefix.length());
    if (file != null)
      dataset = this.connectorApiService.addDataset(file, dto.getFilename(), dto.getDescription(), keywordsList);

    if (dataset != null) {

      // Add dataset_id, policy and distribution format to data_send parameters
      if (dataset.getId() != null) {
        parameters.get("data_send").put("dataset_id", dataset.getId());
      }

      if (dataset.getDistribution() != null && !dataset.getDistribution().isEmpty()) {
        Distribution firstDistribution = dataset.getDistribution().get(0);
        if (firstDistribution != null && firstDistribution.getFormat() != null
            && firstDistribution.getFormat().getId() != null)
          parameters.get("data_send").put("format", firstDistribution.getFormat().getId());
      }

      if (dataset.getHasPolicy() != null && !dataset.getHasPolicy().isEmpty()) {
        Offer firstPolicy = dataset.getHasPolicy().get(0);
        if (firstPolicy != null) {
          ObjectMapper objectMapper = new ObjectMapper();
          String firstPolicyJson = objectMapper.writeValueAsString(firstPolicy);
          parameters.get("data_send").put("policy", firstPolicyJson);
        }
      }

      /* Save Data Offering to Central Registry */
      id = this.provideDataRestTemplate.post(parameters, headers);

      // Add entity id and file to data_send parameters
      parameters.get("data_send").put("id", id);
      parameters.get("data_send").put("message", "data:application/octet-stream;base64," + dto.getFile());

      /* Send data to push service */
      pushServiceResCode = this.entityService.postObjectToPushUri(parameters, headers);
    }

    if (pushServiceResCode != null && !Integer.valueOf(0).equals(pushServiceResCode)) {
      return new FormResponse(id, pushServiceResCode.toString());
    } else {
      return new FormResponse(id);
    }
  }

  public void delete(String id, Map<String, String> headers) {
    try {
      /* Retrieve data info from Central Registry */
      String datasetId = null;
      Map<String, Object> data = this.provideDataRestTemplate.getById(headers, id);
      if (data != null) {
        Map<String, Object> dataSendObj = (Map<String, Object>) data.get("data_send_obj");
        if (dataSendObj == null || dataSendObj.get("id") == null || !id.equals(dataSendObj.get("id"))) {
          throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Data with id [" + id + "] not found or invalid.");
        } else {
          log.info("Data with id [{}] retrieved from Central Registry.", id);
          datasetId = (String) dataSendObj.get("dataset_id");
        }
        /* Delete dataset from local DSP True Connector */
        if (datasetId != null) {
          this.connectorApiService.deleteDataset(datasetId);
        }
        /* Delete data from Central Registry */
        this.provideDataRestTemplate.delete(headers, id);
        log.info("Data with id [{}] successfully deleted from Central Registry.", id);
      }
    } catch (Exception e) {
      log.error("Error deleting data with id [{}]", id);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
    }
  }

}
