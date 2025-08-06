package it.eng.onenet.dsp.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class FormResponse {

    public FormResponse(String id){
        this.id = id;
    }
    // new constructor to integrate the "push_uri" request response
    public FormResponse(String id , String responseCode){
        this.id = id;
        this.responseCode = responseCode;
    }

    public String id;
    public String responseCode;

}
