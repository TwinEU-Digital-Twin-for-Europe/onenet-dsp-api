package it.eng.onenet.dsp.api.dto.connector;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Agreement {
    @JsonProperty("@id")
    private String id;
    private String assignee;
    private String assigner;
    private List<Permission> permission;
    private String target;
    private String timestamp;
}