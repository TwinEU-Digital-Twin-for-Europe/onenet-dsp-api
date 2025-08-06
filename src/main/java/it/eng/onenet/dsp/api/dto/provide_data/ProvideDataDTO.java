package it.eng.onenet.dsp.api.dto.provide_data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class ProvideDataDTO {
    String title;
    String description;
    String filename;
    String file;
    String fileSize;

    @NotEmpty
    @NotNull(message = "Data Offering Id not null")
    String data_offering_id;
    String code;
}
