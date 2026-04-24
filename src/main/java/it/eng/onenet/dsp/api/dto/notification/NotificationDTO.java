package it.eng.onenet.dsp.api.dto.notification;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class NotificationDTO {

	private NotificationUserDTO fromUser;

	private NotificationUserDTO toUser;

	private NotificationType type;

	private String title;

	private String body;

	private Map<String, Object> payload;

}
