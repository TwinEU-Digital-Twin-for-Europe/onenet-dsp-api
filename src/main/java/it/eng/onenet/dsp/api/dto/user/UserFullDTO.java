package it.eng.onenet.dsp.api.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class UserFullDTO {

	@JsonProperty("user_obj")
	private UserWithCompanyDTO user;

}
