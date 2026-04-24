package it.eng.onenet.dsp.api.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataProvidedEventDTO {

	private String dataEntityId;

	private String dataEntityTitle;

	private String dataOfferingId;

	private String dataOfferingType;
}
