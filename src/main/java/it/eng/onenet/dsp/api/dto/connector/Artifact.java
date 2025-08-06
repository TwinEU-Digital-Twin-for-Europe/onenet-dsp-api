package it.eng.onenet.dsp.api.dto.connector;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Artifact {
    @JsonProperty("@id")
    public String id;
    public String artifactType;
    public String authorization;
    public String contentType;
    public String created;
    public String createdBy;
    public String filename;
    public String lastModifiedBy;
    public String lastModifiedDate;
    public String value;
    public int version;
}