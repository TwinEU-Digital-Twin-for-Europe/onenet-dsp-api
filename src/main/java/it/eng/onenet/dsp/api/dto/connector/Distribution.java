package it.eng.onenet.dsp.api.dto.connector;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Distribution {
    @JsonProperty("@id")
    private String id;
    private List<DataService> accessService;
    private String createdBy;
    private Object description;
    private Format format;
    private Object hasPolicy;
    private String issued;
    private String lastModifiedBy;
    private String modified;
    private String title;
    private String type;
    private int version;
}