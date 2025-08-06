package it.eng.onenet.dsp.api.service.user;

import it.eng.onenet.dsp.api.dto.user.JwtAuthenticationResponseDTO;
import it.eng.onenet.dsp.api.dto.user.LoginDTO;
import it.eng.onenet.dsp.api.dto.user.UserDTO;
import it.eng.onenet.dsp.api.rest_template.user.UserRestTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserService {

    private final UserRestTemplate userRestTemplate;

    public UserService(UserRestTemplate userRestTemplate) {
        this.userRestTemplate = userRestTemplate;
    }

    public UserDTO getCurrentUser(Map<String, String> headers) {
        return this.userRestTemplate.getCurrentUser(headers);
    }

    public JwtAuthenticationResponseDTO authenticate(LoginDTO loginDTO) {
        return this.userRestTemplate.authenticate(loginDTO);
    }
}
