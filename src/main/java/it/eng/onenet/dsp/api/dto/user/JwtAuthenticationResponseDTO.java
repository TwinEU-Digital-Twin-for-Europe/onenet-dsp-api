package it.eng.onenet.dsp.api.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class JwtAuthenticationResponseDTO {
    private String accessToken;
    private UserDTO user;
}
