package it.eng.onenet.dsp.api.dto.connector;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Catalog {
    @JsonProperty("@id")
    private String id;
    private String conformsTo;
    private String createdBy;
    private String creator;
    private List<Dataset> dataset;
    private List<LangValue> description;
    private List<Distribution> distribution;
    private Object hasPolicy;
    private String homepage;
    private String identifier;
    private String issued;
    private List<String> keyword;
    private String lastModifiedBy;
    private String modified;
    private String participantId;
    private List<DataService> service;
    private List<String> theme;
    private String title;
    private String type;
    private int version;
}
