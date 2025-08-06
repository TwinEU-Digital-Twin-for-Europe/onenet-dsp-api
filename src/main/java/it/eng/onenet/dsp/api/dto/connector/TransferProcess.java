package it.eng.onenet.dsp.api.dto.connector;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class TransferProcess {
  @JsonProperty("@id")
  private String id;
  private String agreementId;
  private String callbackAddress;
  private String consumerPid;
  private String created;
  private String createdBy;
  private DataAddress dataAddress;
  private String dataId;
  private String datasetId;
  private String format;
  private boolean isDownloaded;
  private String lastModifiedBy;
  private String modified;
  private String providerPid;
  private String role;
  private String state;
  private int version;
}
