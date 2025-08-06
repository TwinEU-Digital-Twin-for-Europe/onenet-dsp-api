package it.eng.onenet.dsp.api.dto.user;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class LoginDTO {
    private String username;
    private String password;
}
