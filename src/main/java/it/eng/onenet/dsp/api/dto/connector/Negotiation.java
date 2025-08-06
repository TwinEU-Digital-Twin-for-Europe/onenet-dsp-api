package it.eng.onenet.dsp.api.dto.connector;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Negotiation {
  @JsonProperty("@id")
  private String id;
  private Agreement agreement;
  private String assigner;
  private String callbackAddress;
  private String consumerPid;
  private String created;
  private String createdBy;
  private Offer offer;
  private String lastModifiedBy;
  private String modified;
  private String providerPid;
  private String role;
  private String state;
  private int version;
}
