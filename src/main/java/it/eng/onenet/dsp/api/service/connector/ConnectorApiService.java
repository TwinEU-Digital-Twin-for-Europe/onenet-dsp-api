package it.eng.onenet.dsp.api.service.connector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import it.eng.onenet.dsp.api.dto.connector.Catalog;
import it.eng.onenet.dsp.api.dto.connector.ConnectorResponse;
import it.eng.onenet.dsp.api.dto.connector.DataService;
import it.eng.onenet.dsp.api.dto.connector.Dataset;
import it.eng.onenet.dsp.api.dto.connector.Distribution;
import it.eng.onenet.dsp.api.dto.connector.Negotiation;
import it.eng.onenet.dsp.api.dto.connector.OuterResponse;
import it.eng.onenet.dsp.api.dto.connector.PagedResponse;
import it.eng.onenet.dsp.api.dto.connector.TransferProcess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ConnectorApiService {

  @Autowired
  private ObjectMapper objectMapper;

  private RestTemplate restTemplate;
  private ResourceLoader resourceLoader;

  @Value("${jsonObjects.folder.path}")
  private String jsonObjectsFolderPath;

  @Value("${connector.basic.auth}")
  private String connectorBasicAuth;

  @Value("${connector.endpoint.url}")
  private String connectorEndpointUrl;

  @Value("${connector.endpoint.api.url}")
  private String connectorEndpointApiUrl;

  @Value("${connector.api}")
  private String connectorApi;

  public ConnectorApiService(RestTemplate restTemplate, ResourceLoader resourceLoader) {
    this.restTemplate = restTemplate;
    this.resourceLoader = resourceLoader;
  }

  public Catalog getCatalog() {
    HttpEntity<String> request = createHttpEntityRequest(null);

    ResponseEntity<ConnectorResponse<Catalog>> response = restTemplate.exchange(
        String.format("%scatalogs", connectorEndpointApiUrl), HttpMethod.GET, request,
        new ParameterizedTypeReference<ConnectorResponse<Catalog>>() {
        });
    return response.getBody().getData();
  }

  public void addCatalog() throws IOException {
    log.info("Adding Catalog...");
    HttpEntity<String> request = createHttpEntityRequest(getJsonPayloadFromResource("jsonObjects/catalog.json"));
    ResponseEntity<ConnectorResponse<Catalog>> response = restTemplate.exchange(
        String.format("%scatalogs", connectorEndpointApiUrl), HttpMethod.POST, request,
        new ParameterizedTypeReference<ConnectorResponse<Catalog>>() {
        });

    String newCatalogId = response.getBody().getData().getId();
    log.info(String.format("Catalog with id [%s] created.", newCatalogId));
  }

  public Distribution getDistribution(String id) {
    HttpEntity<String> request = createHttpEntityRequest(null);

    ResponseEntity<ConnectorResponse<Distribution>> response = restTemplate.exchange(
        String.format("%sdistributions/%s", connectorEndpointApiUrl, id), HttpMethod.GET, request,
        new ParameterizedTypeReference<ConnectorResponse<Distribution>>() {
        });
    return response.getBody().getData();
  }

  public void addDistribution(String dataServiceId) throws IOException {
    log.info("Adding Distribution...");
    String distributionPayLoad = getJsonPayloadFromResource("jsonObjects/distribution.json");
    JsonNode root = objectMapper.readTree(distributionPayLoad);

    JsonNode accessServiceNode = root.get("accessService");

    if (accessServiceNode.isArray() && accessServiceNode.size() > 0) {
      ObjectNode firstAccessService = (ObjectNode) accessServiceNode.get(0);
      firstAccessService.put("@id", dataServiceId);
    }
    HttpEntity<String> request = createHttpEntityRequest(objectMapper.writeValueAsString(root));
    ResponseEntity<ConnectorResponse<Distribution>> response = restTemplate.exchange(
        String.format("%sdistributions", connectorEndpointApiUrl), HttpMethod.POST, request,
        new ParameterizedTypeReference<ConnectorResponse<Distribution>>() {
        });
    String newDistributionId = response.getBody().getData().getId();
    log.info(String.format("Distribution with id [%s] created.", newDistributionId));
  }

  public String addDataService() throws IOException {
    log.info("Adding DataService...");
    String dataServicePayLoad = getJsonPayloadFromResource("jsonObjects/dataService.json");
    // replace endpointURL
    log.info(String.format("Using endpointURL: %s", connectorEndpointUrl));

    JsonNode root = objectMapper.readTree(dataServicePayLoad);
    ((ObjectNode) root).put("endpointURL", connectorEndpointUrl);

    HttpEntity<String> request = createHttpEntityRequest(objectMapper.writeValueAsString(root));

    ResponseEntity<ConnectorResponse<DataService>> response = restTemplate.exchange(
        String.format("%sdataservices", connectorEndpointApiUrl), HttpMethod.POST, request,
        new ParameterizedTypeReference<ConnectorResponse<DataService>>() {
        });

    String newDataServiceId = response.getBody().getData().getId();
    log.info(String.format("DataService with id [%s] created.", newDataServiceId));
    return newDataServiceId;
  }

  public DataService UpdateDataService(String id, DataService dataService) {
    log.info(String.format("DataService with id [%s] updating...", id));
    HttpEntity<DataService> request = createHttpEntityRequest(dataService);
    ResponseEntity<ConnectorResponse<DataService>> response = restTemplate.exchange(
        String.format("%sdataservices/%s", connectorEndpointApiUrl, id), HttpMethod.PUT, request,
        new ParameterizedTypeReference<ConnectorResponse<DataService>>() {
        });
    return response.getBody().getData();
  }

  public Dataset addDataset(String file, String filename, String description, List<String> keywords) throws IOException {
    log.info("Adding Dataset...");
    Dataset newDataset = null;
    // TODO: Set user's e-mail or username in the creator field
    String creator = "One-Net Connector";

    log.info("Retrieving catalog info...");
    Catalog catalog = this.getCatalog();
    if (catalog != null && catalog.getDistribution() != null && !catalog.getDistribution().isEmpty()) {
      Distribution distribution = catalog.getDistribution().get(0);
      if (distribution != null) {
        // Load the default json dataset
        String datasetPayLoad = getJsonPayloadFromResource("jsonObjects/dataset.json");
        // Replace dataset values at the root level
        JsonNode root = objectMapper.readTree(datasetPayLoad);
        ObjectNode rootObject = (ObjectNode) root;
        if (rootObject != null && rootObject.isObject()) {
          rootObject.put("creator", creator);
          rootObject.put("title", filename);
          // Replace dataset nested values - description
          JsonNode descriptionNode = rootObject.get("description");
          if (descriptionNode.isArray() && descriptionNode.size() > 0) {
            ObjectNode firstDescription = (ObjectNode) descriptionNode.get(0);
            if (firstDescription != null && firstDescription.isObject()) {
              firstDescription.put("value", description);
            }
          }
          // Replace dataset nested values - distribution
          ArrayNode distributionNodeArray = (ArrayNode) rootObject.get("distribution");
          if (distributionNodeArray != null && distributionNodeArray.isArray()) {
            JsonNode distributionNode = objectMapper.valueToTree(distribution);
            distributionNodeArray.add(distributionNode);
          }
          // Replace dataset nested values - keywords
          ArrayNode keywordNodeArray = (ArrayNode) rootObject.get("keyword");
          if (keywordNodeArray != null && keywordNodeArray.isArray()) {
            for (String keyword : keywords) {
              keywordNodeArray.add(keyword);
            }
          }
        }

        HttpEntity<MultiValueMap<String, Object>> request = createMultipartHttpEntityRequest(
            objectMapper.writeValueAsString(root), file, filename);

        try {
          ResponseEntity<ConnectorResponse<Dataset>> response = restTemplate.exchange(
              String.format("%sdatasets", connectorEndpointApiUrl), HttpMethod.POST, request,
              new ParameterizedTypeReference<ConnectorResponse<Dataset>>() {
              });

          newDataset = response.getBody().getData();
        } catch (RestClientException e) {
          throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Dataset cannot be saved.");
        }
      }
    }
    if (newDataset != null)
      log.info(String.format("Dataset with id [%s] created.", newDataset.getId()));
    else
      log.info("Dataset not created.");
    return newDataset;
  }

  public void deleteDataset(String datasetId) {
    log.info("Deleting Dataset with id [{}]...", datasetId);
    try {
      HttpEntity<String> request = createHttpEntityRequest(null);
      ResponseEntity<ConnectorResponse<Void>> response = restTemplate.exchange(
          String.format("%sdatasets/%s", connectorEndpointApiUrl, datasetId),
          HttpMethod.DELETE,
          request,
          new ParameterizedTypeReference<ConnectorResponse<Void>>() {
          });
      if (response.getStatusCode().is2xxSuccessful())
        log.info("Dataset with id [{}] deleted successfully.", datasetId);
    } catch (RestClientException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Dataset could not be deleted.");
    }
  }

  public Negotiation startNegotiation(String policy, String datasetId, String providerConnectorUrl)
      throws JsonProcessingException {

    log.info("[C] Starting Contract Negotiation...");

    Negotiation negotiation = null;

    Map<String, Object> policyObj = objectMapper.readValue(policy, new TypeReference<Map<String, Object>>() {
    });

    Map<String, Object> offer = new HashMap<>(policyObj);
    offer.put("target", datasetId);
    offer.put("assigner", "urn:uuid:ASSIGNER_PROVIDER");

    // DEBUG - set internal provider connector url
    // providerConnectorUrl = "http://host.docker.internal:8090";

    Map<String, Object> payload = new HashMap<>();
    payload.put("Forward-To", providerConnectorUrl);
    payload.put("offer", offer);

    String jsonPayload = objectMapper.writeValueAsString(payload);

    HttpEntity<String> request = createHttpEntityRequest(jsonPayload);

    try {
      ResponseEntity<ConnectorResponse<Negotiation>> response = restTemplate.exchange(
          String.format("%snegotiations", connectorEndpointApiUrl), HttpMethod.POST, request,
          new ParameterizedTypeReference<ConnectorResponse<Negotiation>>() {
          });

      negotiation = response.getBody().getData();
    } catch (RestClientException e) {
      log.error(e.getMessage(), e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Contract Negotiation not started.");
    }

    if (negotiation != null)
      log.info(String.format("Contract Negotiation with id [%s] started.", negotiation.getId()));
    else
      log.info("Contract Negotiation not started.");

    return negotiation;
  }

  public List<Negotiation> findNegotiations(String connectorUrl, String providerPid, String consumerPid, String role) {

    List<Negotiation> negotiations = null;
    String sender = "";

    if (role.equals("consumer")) {
      sender = "C";
      connectorUrl = connectorEndpointApiUrl;
    } else {
      if (role.equals("provider")) {
        sender = "P";
        connectorUrl += connectorApi;
      }
    }

    log.info("[{}] Finding Contract Negotiations...", sender);

    HttpEntity<String> request = createHttpEntityRequest(null);

    try {
      ResponseEntity<OuterResponse<PagedResponse<Negotiation>>> response = restTemplate.exchange(
          String.format("%snegotiations?role=%s&consumerPid=%s&providerPid=%s", connectorUrl, role, consumerPid, providerPid),
          HttpMethod.GET, request, new ParameterizedTypeReference<OuterResponse<PagedResponse<Negotiation>>>() {
          });

      negotiations = response.getBody().getResponse().getData().getContent();
    } catch (RestClientException e) {
      log.error("Error while retrieving Contract Negotiations: {}", e.getMessage());
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No Contract Negotiations found.");
    }

    if (negotiations != null && !negotiations.isEmpty())
      log.info(String.format("Contract Negotiations found for consumerPid [%s] and providerPid [%s].", consumerPid, providerPid));
    else
      log.info("No Contract Negotiations found.");

    return negotiations;
  }

  public void sendNegotiationRequest(String operation, String negotiationId, String connectorUrl) {

    switch (operation) {
    case "approve":
      log.info("[P] Approving negotiation...");
      connectorUrl += connectorApi;
      break;
    case "verify":
      log.info("[C] Verifing negotiation...");
      connectorUrl = connectorEndpointApiUrl;
      break;
    case "finalize":
      log.info("[P] Finalising negotiation...");
      connectorUrl += connectorApi;
      break;
    default:
      log.info("Sending Negotiation Request. Unknown operation.");
      return;
    }

    HttpEntity<String> request = createHttpEntityRequest(null);

    try {
      ResponseEntity<ConnectorResponse<Negotiation>> response = restTemplate.exchange(
          String.format("%snegotiations/%s/%s", connectorUrl, negotiationId, operation), HttpMethod.PUT, request,
          new ParameterizedTypeReference<ConnectorResponse<Negotiation>>() {
          });

      log.info(response.getBody().getMessage());
    } catch (RestClientException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Contract Negotiation request failed.");
    }
  }

  public TransferProcess findTransferProcess(String transferProcessId) {

    TransferProcess transferProcess = null;

    log.info("Finding Transfer Process with id [{}] ...", transferProcessId);

    HttpEntity<String> request = createHttpEntityRequest(null);

    ResponseEntity<ConnectorResponse<TransferProcess>> response = restTemplate.exchange(
        String.format("%stransfers/%s", connectorEndpointApiUrl, transferProcessId), HttpMethod.GET, request,
        new ParameterizedTypeReference<ConnectorResponse<TransferProcess>>() {
        });
    transferProcess = response.getBody().getData();

    if (transferProcess != null)
      log.info("Transfer Process found.");
    else
      log.info("No Transfer Process found.");

    return transferProcess;
  }

  public List<TransferProcess> findTransferProcesses(String connectorUrl, String state, String role, String datasetId,
      String providerPid, String consumerPid) {

    List<TransferProcess> transferProcesses = null;
    String sender = "";

    if (connectorUrl == null) {
      sender = "C";
      connectorUrl = connectorEndpointApiUrl;
    } else {
      sender = "P";
      connectorUrl += connectorApi;
    }
    log.info("[{}] Finding {} Transfer Processes...", sender, state);

    HttpEntity<String> request = createHttpEntityRequest(null);

    try {
      UriComponentsBuilder builder = UriComponentsBuilder
          .fromHttpUrl(connectorUrl + "transfers");

      if (state != null && !state.isEmpty()) {
        builder.queryParam("state", state);
      }
      if (role != null && !role.isEmpty()) {
        builder.queryParam("role", role);
      }
      if (datasetId != null && !datasetId.isEmpty()) {
        builder.queryParam("datasetId", datasetId);
      }
      if (providerPid != null && !providerPid.isEmpty()) {
        builder.queryParam("providerPid", providerPid);
      }
      if (consumerPid != null && !consumerPid.isEmpty()) {
        builder.queryParam("consumerPid", consumerPid);
      }

      String url = builder.toUriString();

      ResponseEntity<OuterResponse<PagedResponse<TransferProcess>>> response = restTemplate.exchange(url, HttpMethod.GET, request,
          new ParameterizedTypeReference<OuterResponse<PagedResponse<TransferProcess>>>() {
          });

      transferProcesses = response.getBody().getResponse().getData().getContent();
    } catch (RestClientException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No Transfer Processes found.");
    }

    if (transferProcesses != null && !transferProcesses.isEmpty())
      log.info(String.format("Transfer Processes found for datasetId [%s], consumerPid [%s] and providerPid [%s].",
          datasetId, consumerPid, providerPid));
    else
      log.info("No Transfer Process found.");

    return transferProcesses;
  }

  public TransferProcess requestTransferAsConsumer(String transferProcessId, String format)
      throws JsonProcessingException {

    log.info("[C] Requesting Data Transfer...");

    TransferProcess transfer = null;

    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("transferProcessId", transferProcessId);
    payload.put("format", format);

    String jsonPayload = objectMapper.writeValueAsString(payload);

    HttpEntity<String> request = createHttpEntityRequest(jsonPayload);

    try {
      ResponseEntity<ConnectorResponse<TransferProcess>> response = restTemplate.exchange(
          String.format("%stransfers", connectorEndpointApiUrl), HttpMethod.POST, request,
          new ParameterizedTypeReference<ConnectorResponse<TransferProcess>>() {
          });

      transfer = response.getBody().getData();
    } catch (RestClientException e) {
      log.error("Error while requesting Data Transfer: {}", e.getMessage());
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Data Transfer request failed.");
    }

    if (transfer != null)
      log.info(String.format("Data Transfer request with id [%s] created.", transfer.getId()));
    else
      log.info("Data Transfer request not created.");

    return transfer;
  }

  public byte[] sendTransferRequest(String operation, String transferProcessId, String connectorUrl) {

    byte[] data = null;
    HttpMethod httpMethod = null;

    switch (operation) {
    case "start":
      log.info("[P] Starting Transfer...");
      connectorUrl += connectorApi;
      httpMethod = HttpMethod.PUT;
      break;
    case "download":
      log.info("[C] Downloading Data...");
      connectorUrl = connectorEndpointApiUrl;
      httpMethod = HttpMethod.GET;
      break;
    case "complete":
      log.info("[C] Completing Transfer...");
      connectorUrl = connectorEndpointApiUrl;
      httpMethod = HttpMethod.PUT;
      break;
    case "view":
      log.info("[C] Viewing Data...");
      connectorUrl = connectorEndpointApiUrl;
      httpMethod = HttpMethod.GET;
      break;
    default:
      log.info("Sending Transfer Request. Unknown operation.");
      return data;
    }

    HttpEntity<String> request = createHttpEntityRequest(null);

    String url = String.format("%stransfers/%s/%s", connectorUrl, transferProcessId, operation);

    if (operation.equals("view")) {

      try {
        ResponseEntity<byte[]> response = restTemplate.exchange(url, httpMethod, request, byte[].class);

        data = response.getBody();
      } catch (RestClientException e) {
        log.error("Error while retrieving transferred file: {}", e.getMessage());
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
            "Data Transfer failed - File cannot be retrieved.");
      }

      if (data != null)
        log.info(String.format("Data Transfer completed."));
      else
        log.info("Data Transfer failed.");

    } else {
      try {
        ResponseEntity<ConnectorResponse<Object>> response = restTemplate.exchange(url, httpMethod, request,
            new ParameterizedTypeReference<ConnectorResponse<Object>>() {
            });

        log.info(response.getBody().getMessage());
      } catch (RestClientException e) {
        log.error("Error while retrieving transferred file: {}", e.getMessage());
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
            "Data Transfer failed - File cannot be retrieved.");
      }
    }

    return data;
  }

  private String getJsonPayloadFromResource(String resourcePath) throws IOException {
    Resource resource = resourceLoader.getResource(jsonObjectsFolderPath + resourcePath);
    byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private HttpEntity<String> createHttpEntityRequest(String jsonPayload) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBasicAuth(connectorBasicAuth);

    HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);
    return request;
  }

  private <T> HttpEntity<T> createHttpEntityRequest(T obj) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBasicAuth(connectorBasicAuth);

    HttpEntity<T> request = new HttpEntity<>(obj, headers);
    return request;
  }

  private HttpEntity<MultiValueMap<String, Object>> createMultipartHttpEntityRequest(String jsonPayload,
      String base64File, String filename) {

    // Decode file from Base64
    byte[] fileBytes = Base64.getDecoder().decode(base64File);

    // Set headers
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.setBasicAuth(connectorBasicAuth);

    // Create the json part of the request
    HttpEntity<String> jsonRequest = new HttpEntity<>(jsonPayload, headers);

    // Create the file part of the request
    ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
      @Override
      public String getFilename() {
        return filename;
      }
    };

    // Crete the multipart body of the request
    MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
    multipartBody.add("dataset", jsonRequest);
    multipartBody.add("file", fileResource);

    // Create the http request
    HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(multipartBody, headers);

    return request;
  }

}
