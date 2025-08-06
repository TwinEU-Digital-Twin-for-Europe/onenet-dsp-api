package it.eng.onenet.dsp.api.runner;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import it.eng.onenet.dsp.api.dto.connector.Catalog;
import it.eng.onenet.dsp.api.service.connector.ConnectorApiService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class InitDspConnectorService {
    private static final int MAX_ATTEMPTS = 30;
    private static final long WAIT_MS = 5000;

    @Value("${connector.endpoint.api.url}")
    private String connectorEndpointApiUrl;
    private ConnectorApiService connectorApiService;

    public InitDspConnectorService(ConnectorApiService connectorApiService) {
        this.connectorApiService = connectorApiService;
    }

    public boolean isUrlReachable(String urlString, int timeoutMs) {
        try {
            URI uri = URI.create(urlString);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setRequestMethod("HEAD");
            conn.connect();
            return true;
        } catch (IOException e) {
            // timeout, DNS, refused connection…
            return false;
        }
    }

    public void init() throws IOException, InterruptedException {
        int attempt = 0;
        while (attempt < MAX_ATTEMPTS) {
            if (isUrlReachable(connectorEndpointApiUrl, 500)) {
                log.info(String.format("Connector Up on %s", connectorEndpointApiUrl));
                try {
                    this.initConnector();
                    return;
                } catch (Exception e) {
                    // Connector is running, but REST APIs are not ready...
                    if (!(e instanceof ResourceAccessException)) {
                        log.error(e.getMessage(), e);
                    }
                }

            }
            attempt++;
            log.info(String.format("Waiting Connector on [%s] (%d/%d)%n", connectorEndpointApiUrl, attempt,
                    MAX_ATTEMPTS));
            Thread.sleep(WAIT_MS);
        }
        throw new IllegalStateException("Connector not ready!!!");

    }

    private void initConnector() throws IOException {
        log.info(String.format("init connector on %s ...", connectorEndpointApiUrl));

        log.info("check exists Catalog...");
        try {
            Catalog catalog = connectorApiService.getCatalog();
            log.info(String.format("Catalog with id [%s] exists!", catalog.getId()));

            // log.info("check exists Distribution...");
            // if (catalog.getDistribution() != null &&
            // !catalog.getDistribution().isEmpty()) {
            // log.info(String.format("Distribution with id [%s] exists",
            // catalog.getDistribution().get(0).getId()));
            // } else {
            // String dataServiceId = this.addDataService();
            // this.addDistribution(dataServiceId);
            // }
        } catch (HttpClientErrorException.NotFound e) {
            log.info("Catalog not found!");
            connectorApiService.addCatalog();
            String dataServiceId = connectorApiService.addDataService();
            connectorApiService.addDistribution(dataServiceId);
        }

    }
}
