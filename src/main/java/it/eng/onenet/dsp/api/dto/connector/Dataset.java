package it.eng.onenet.dsp.api.dto.connector;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Dataset {
  @JsonProperty("@id")
  private String id;
  private Artifact artifact;
  private String conformsTo;
  private String createdBy;
  private String creator;
  private List<LangValue> description;
  private List<Distribution> distribution;
  private List<Offer> hasPolicy;
  private String identifier;
  private String issued;
  private List<String> keyword;
  private String lastModifiedBy;
  private String modified;
  private List<String> theme;
  private String title;
  private String type;
  private int version;
}