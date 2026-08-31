package it.eng.onenet.dsp.api.service.consume_data;

import it.eng.onenet.dsp.api.dto.FileResponse;
import it.eng.onenet.dsp.api.dto.connector.Negotiation;
import it.eng.onenet.dsp.api.dto.connector.TransferProcess;
import it.eng.onenet.dsp.api.dto.list_results.ListResultsDataDTO;
import it.eng.onenet.dsp.api.rest_template.consume_data.ConsumeDataRestTemplate;
import it.eng.onenet.dsp.api.service.connector.ConnectorApiService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class ConsumeDataService {

    @Value("${transfer.poll.max-retries}")
    private int maxRetries;

    @Value("${transfer.poll.delay-millis}")
    private int delayMillis;

    private final ConsumeDataRestTemplate consumeDataRestTemplate;
    private final ConnectorApiService connectorApiService;
    private final ObjectMapper objectMapper;

    public ConsumeDataService(ConsumeDataRestTemplate consumeDataRestTemplate,
            ConnectorApiService connectorApiService, ObjectMapper objectMapper) {
        this.consumeDataRestTemplate = consumeDataRestTemplate;
        this.connectorApiService = connectorApiService;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> getList(Map<String, String> headers) {
        return this.consumeDataRestTemplate.getList(headers);
    }

    public ListResultsDataDTO getPage(Map<String, String> headers, Long page) {
        return this.consumeDataRestTemplate.getPage(headers, page);
    }

    public Map<String, Object> getEntity(Map<String, String> headers, String id) {
        return this.consumeDataRestTemplate.getEntity(headers, id);
    }

    public Map<String, String> retrieveFileMetadata(Map<String, String> headers, String id) {

        Map<String, Object> entity = this.consumeDataRestTemplate.getEntity(headers, id);

        if (entity != null) {
            Map<String, Object> dataSendObj = (Map<String, Object>) entity.get("data_send_obj");
            if (dataSendObj != null) {
                String format = (String) dataSendObj.get("format");
                String policy = (String) dataSendObj.get("policy");
                String datasetId = (String) dataSendObj.get("dataset_id");

                Map<String, Object> providerObj = (Map<String, Object>) dataSendObj.get("provider_data_obj");
                if (providerObj != null) {
                    String providerLocalApiUrl = (String) providerObj.get("local_api_url");
                    String providerConnectorUrl = (String) providerObj.get("connector_url");
                    String providerId = (String) providerObj.get("id");

                    if (format != null && policy != null && datasetId != null && providerLocalApiUrl != null
                            && providerConnectorUrl != null) {

                        Map<String, String> metadata = new HashMap<>();
                        metadata.put("format", format);
                        metadata.put("policy", policy);
                        metadata.put("datasetId", datasetId);
                        metadata.put("providerLocalApiUrl", providerLocalApiUrl);
                        metadata.put("providerConnectorUrl", providerConnectorUrl);
                        metadata.put("providerId", providerId);
                        return metadata;
                    }
                }
            }
        }
        return null;
    }

    public FileResponse getFile(Map<String, String> headers, String id) throws IOException, InterruptedException {

        FileResponse fileResponse = null;
        String encodedData = null;
        byte[] data = null;

        /* Retrieve file metadata from central registry */
        Map<String, String> metadata = this.retrieveFileMetadata(headers, id);
        if (metadata == null)
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File cannot be retrieved.");

        /* Get file from provider's connector */

        // Check whether the file has already been downloaded once - no need for
        // negotiation
        Optional<byte[]> optionalData = viewData(metadata);
        if (optionalData.isPresent()) {
            data = optionalData.get();
        } else {
            /* Negotiation process */
            this.negotiate(metadata);

            /* Data transfer process */
            data = this.transferData(metadata);
        }

        // Download file from s3
        if (data != null) {
            data = this.downloadFileFromS3(data);
        }

        // Encode in Base64
        if (data != null)
            encodedData = Base64.getEncoder().encodeToString(data);

        if (encodedData != null && !encodedData.isEmpty()) {
            fileResponse = new FileResponse(encodedData, true);
        } else
            fileResponse = new FileResponse(null, false);

        return fileResponse;
    }

    public Object getJSONFIle(Map<String, String> headers, String id) throws IOException, InterruptedException {
        FileResponse fileResponse = this.getFile(headers, id);
        if (fileResponse != null && fileResponse.retrieved) {
            String base64Data = fileResponse.getFiledata();
            byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
            String jsonString = new String(decodedBytes, StandardCharsets.UTF_8);
            return objectMapper.readValue(jsonString, Object.class);
        }
        return null;
    }

    private void negotiate(Map<String, String> metadata) throws JsonProcessingException {

        String policy = metadata.get("policy");
        String datasetId = metadata.get("datasetId");
        String providerConnectorUrl = metadata.get("providerConnectorUrl");

        List<Negotiation> providerNegotiations = null;
        Negotiation consumerNegotiation = null;
        Negotiation providerNegotiation = null;
        String consumerNegotiationId = null;
        String providerNegotiationId = null;
        String consumerPid = null;
        String providerPid = null;

        // [C] Start Negotiation
        consumerNegotiation = this.connectorApiService.startNegotiation(policy, datasetId, providerConnectorUrl);

        if (consumerNegotiation != null) {
            consumerNegotiationId = consumerNegotiation.getId();
            consumerPid = consumerNegotiation.getConsumerPid();
            providerPid = consumerNegotiation.getProviderPid();
        }

        if (consumerPid != null && providerPid != null) {
            // [P] Find Contract Negotiations
            providerNegotiations = this.connectorApiService.findNegotiations(providerConnectorUrl, providerPid,
                    consumerPid,
                    "provider");
        }

        if (providerNegotiations != null && !providerNegotiations.isEmpty())
            providerNegotiation = providerNegotiations.get(0);

        if (providerNegotiation != null) {
            providerNegotiationId = providerNegotiation.getId();
        }

        if (providerNegotiationId != null && consumerNegotiationId != null) {

            // [P] Approve
            this.connectorApiService.sendNegotiationRequest("approve", providerNegotiationId, providerConnectorUrl);

            // [C] Verify
            this.connectorApiService.sendNegotiationRequest("verify", consumerNegotiationId, null);

            // [P] Finalise
            this.connectorApiService.sendNegotiationRequest("finalize", providerNegotiationId, providerConnectorUrl);
        }
    }

    public byte[] transferData(Map<String, String> metadata) throws JsonProcessingException, InterruptedException {

        String datasetId = metadata.get("datasetId");
        String format = metadata.get("format");
        String providerConnectorUrl = metadata.get("providerConnectorUrl");

        byte[] data = null;

        List<TransferProcess> consumerProcesses = null;
        TransferProcess consumerTransferProcess = null;
        String consumerTransferProcessId = null;
        String consumerPid = null;
        List<TransferProcess> providerProcesses = null;
        TransferProcess providerTransferProcess = null;
        String providerTransferProcessId = null;
        String providerPid = null;
        TransferProcess transferRequest = null;

        // [C] Find Initialized Transfer Process
        consumerProcesses = this.connectorApiService.findTransferProcesses(null, "INITIALIZED", "consumer", datasetId,
                null,
                null);

        consumerTransferProcess = (consumerProcesses != null && !consumerProcesses.isEmpty()) ? consumerProcesses.get(0)
                : null;

        if (consumerTransferProcess != null) {
            consumerTransferProcessId = consumerTransferProcess.getId();
            consumerPid = consumerTransferProcess.getConsumerPid();
        }

        // [C] Request Transfer Process
        if (consumerTransferProcessId != null)
            transferRequest = this.connectorApiService.requestTransferAsConsumer(consumerTransferProcessId, format);

        if (transferRequest != null)
            providerPid = transferRequest.getProviderPid();

        // [P] Find Requested Transfer Process
        providerProcesses = this.connectorApiService.findTransferProcesses(providerConnectorUrl, "REQUESTED",
                "provider",
                datasetId, providerPid, consumerPid);

        providerTransferProcess = (providerProcesses != null && !providerProcesses.isEmpty()) ? providerProcesses.get(0)
                : null;

        if (providerTransferProcess != null)
            providerTransferProcessId = providerTransferProcess.getId();

        // [P] Start transfer
        if (providerTransferProcessId != null)
            this.connectorApiService.sendTransferRequest("start", providerTransferProcessId, providerConnectorUrl);

        if (consumerTransferProcessId != null) {
            // [C] Download data
            this.connectorApiService.sendTransferRequest("download", consumerTransferProcessId, null);

            // Wait for download to complete
            this.awaitDataDownload(consumerTransferProcessId);

            // [C] Complete transfer
            this.connectorApiService.sendTransferRequest("complete", consumerTransferProcessId, null);

            // [C] View data
            data = this.connectorApiService.sendTransferRequest("view", consumerTransferProcessId, null);
        }

        return data;
    }

    private void awaitDataDownload(String transferProcessId) throws InterruptedException {

        for (int i = 0; i < maxRetries; i++) {
            TransferProcess process = connectorApiService.findTransferProcess(transferProcessId);

            if (process != null) {
                String state = process.getState();
                Boolean downloaded = process.isDownloaded();

                if (state != null && "STARTED".equals(state) && downloaded != null && downloaded) {
                    log.info("Data download completed for transfer process with id [{}]", transferProcessId);
                    return;
                }
            }

            Thread.sleep(delayMillis);
        }

        log.error("Data download failed or timed out for transfer process with id [{}]", transferProcessId);
        throw new RuntimeException("Data download failed or transfer process timed out.");
    }

    public Optional<byte[]> viewData(Map<String, String> metadata) throws JsonProcessingException {
        String datasetId = metadata.get("datasetId");

        if (datasetId == null) {
            return Optional.empty();
        }

        List<TransferProcess> consumerProcesses = null;
        TransferProcess consumerTransferProcess = null;
        String consumerTransferProcessId = null;
        Boolean downloaded = false;

        consumerProcesses = connectorApiService.findTransferProcesses(null, "COMPLETED", "consumer", datasetId, null,
                null);

        consumerTransferProcess = (consumerProcesses != null && !consumerProcesses.isEmpty()) ? consumerProcesses.get(0)
                : null;

        if (consumerTransferProcess != null) {
            consumerTransferProcessId = consumerTransferProcess.getId();
            downloaded = consumerTransferProcess.isDownloaded();
        }

        if (consumerTransferProcessId != null && Boolean.TRUE.equals(downloaded)) {
            byte[] data = connectorApiService.sendTransferRequest("view", consumerTransferProcessId, null);
            return Optional.ofNullable(data);
        }

        return Optional.empty();
    }

    public byte[] downloadFileFromS3(byte[] data) {

        log.info("Downloading file from s3...");
        byte[] file = null;

        try {

            String link = new String(data, StandardCharsets.UTF_8);

            if (link != null && !link.isEmpty()) {
                log.info("Link decoded: {}", link);

                URI uri = URI.create(link);
                URL url = uri.toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                try (InputStream in = connection.getInputStream();
                        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }

                    file = out.toByteArray();
                }
            }
        } catch (Exception e) {
            log.error("Error downloading file from S3.", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }

        log.info("File correctly downloaded from s3.");
        return file;
    }

}
