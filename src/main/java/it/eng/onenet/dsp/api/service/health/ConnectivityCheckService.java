package it.eng.onenet.dsp.api.service.health;

import it.eng.onenet.dsp.api.rest_template.custom_query.CustomQueryRestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ConnectivityCheckService {

  private final CustomQueryRestTemplate customQueryRestTemplate;

  public ConnectivityCheckService(CustomQueryRestTemplate customQueryRestTemplate) {
    this.customQueryRestTemplate = customQueryRestTemplate;
  }

  @GetMapping
  public Map<String, String> check(@RequestHeader Map<String, String> headers) {

    /* Get Provider Parameters From Central Registry */
    log.debug("1. Get Provider Parameters From Central Registry");
    List<Map<String, Object>> parameters = this.customQueryRestTemplate
        .getDataObjects("e48046c9-0b94-41d2-9ad4-206f1604b821", Collections.EMPTY_MAP, headers);

    log.debug("Response= " + parameters.toString());

    if (parameters.size() <= 0) {
      log.debug("Return 1 - Error retrieving connection settings from central registry");
      return Map.of("code", "1",
          "message", "Error retrieving connection settings from central registry");
    }

    log.debug("Return 0 - Connection settings successfully retrieved from Central Registry");
    return Map.of("code", "0",
        "message", "Connection settings successfully retrieved from Central Registry");
  }

}
