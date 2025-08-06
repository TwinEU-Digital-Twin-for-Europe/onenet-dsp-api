package it.eng.onenet.dsp.api.controller.connector;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import it.eng.onenet.dsp.api.dto.connector.DataService;
import it.eng.onenet.dsp.api.service.connector.ConnectorApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/connector")
@Slf4j
@Tag(name = "Connector API", description = "These endpoints provide the consumed API from the Connector")
public class ConnectorController {
    private ConnectorApiService connectorApiService;

    public ConnectorController(ConnectorApiService connectorApiService) {
        this.connectorApiService = connectorApiService;

    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("dataService")
    public ResponseEntity<DataService> getDataService() {
        List<DataService> dataServices = connectorApiService.getCatalog().getService();
        if (!dataServices.isEmpty()) {
            return ResponseEntity
                    .ok()
                    .body(dataServices.get(0));
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("dataService/{id}")
    public ResponseEntity<DataService> updateDataService(@PathVariable String id,
            @RequestBody DataService newDataService) {
        List<DataService> dataServices = connectorApiService.getCatalog().getService();
        if (!dataServices.isEmpty()) {
            DataService existingDataService = dataServices.get(0);
            if (!id.equals(existingDataService.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT);
            }

            DataService dataServiceUpdated = connectorApiService.UpdateDataService(id, newDataService);
            return ResponseEntity
                    .ok()
                    .body(dataServiceUpdated);
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}