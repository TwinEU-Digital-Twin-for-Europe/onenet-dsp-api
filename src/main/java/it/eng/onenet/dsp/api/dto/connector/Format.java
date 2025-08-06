package it.eng.onenet.dsp.api.dto.connector;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Format {
    @JsonProperty("@id")
    private String id;
}