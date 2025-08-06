package it.eng.onenet.dsp.api.dto.connector;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class DataAddress {

  @JsonProperty("@id")
  private String id;
  private String endpointType;
  private String endpoint;
  private List<Endpoint> endpointProperties;
}
