package it.eng.onenet.dsp.api.service.local_functionality;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import it.eng.onenet.dsp.api.rest_template.PushRestTemplate;
import it.eng.onenet.dsp.api.rest_template.custom_query.CustomQueryRestTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EntityService {

  private final CustomQueryRestTemplate customQueryRestTemplate;
  private final PushRestTemplate pushRestTemplate;

  public EntityService(
      CustomQueryRestTemplate customQueryRestTemplate,
      PushRestTemplate pushRestTemplate) {

    this.customQueryRestTemplate = customQueryRestTemplate;
    this.pushRestTemplate = pushRestTemplate;
  }

  public Integer postObjectToPushUri(Map<String, Map<String, Object>> parameters, Map<String, String> headers) {
    Integer pushRequestResult = 0;
    String dataOfferingType = parameters.get("data_send") != null
        ? String.valueOf(parameters.get("data_send").get("type"))
        : "N/A";
    log.info("Offering type: " + dataOfferingType);
    if ("push".equalsIgnoreCase(dataOfferingType)) {
      String pushUri = parameters.get("data_send") != null
          ? String.valueOf(parameters.get("data_send").get("push_uri"))
          : null;
      /* Check and Send data to push service */
      log.debug("Push_uri from Offering :" + parameters.get("data_send").get("push_uri"));
      if (pushUri != null && !pushUri.isEmpty()) {
        // Call push uri
        log.info("*** Send data to push uri rest service...");
        parameters.get("data_send").put("push_uri", pushUri);
        pushRequestResult = this.pushRestTemplate.post(parameters, headers);
      } else {
        // No push uri defined
        log.info("Push_uri parameter is empty, null, or nonexistent");
      }
    }
    return pushRequestResult;
  }

  public void checkAndSetDataOfferingInfo(Map<String, Map<String, Object>> parameters, Map<String, String> headers) {

    String offeringId = parameters.get("data_send") != null
        ? String.valueOf(parameters.get("data_send").get("data_catalog_data_offerings_id"))
        : null;
    log.info("check offeringId: " + offeringId);

    List<Map<String, Object>> dataOfferingResponse = this.customQueryRestTemplate.getDataObjects(
        "94f1846c-0e0b-42fa-9686-3b5cfa06def1",
        Collections.singletonMap("data_offering_id", offeringId), headers);

    if (dataOfferingResponse.size() <= 0) {
      log.error("Data Offering " + offeringId + " not available in the Central Registry!");
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Data Offering not available");
    }
    log.info("offeringId: " + offeringId + " successfully found in the Central Registry");

    Map<String, Object> response = dataOfferingResponse.get(0);

    parameters.get("data_send").put("type", response.get("type"));
    parameters.get("data_send").put("push_uri", response.get("push_uri"));

    parameters.get("keywords").put("business_object", response.get("business_object_code") + " - " + response.get("business_object_name"));
    parameters.get("keywords").put("service", response.get("service_code") + " - " + response.get("service_name"));
    parameters.get("keywords").put("category", response.get("category_code") + " - " + response.get("category_name"));
  }
}
