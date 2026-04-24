package it.eng.onenet.dsp.api.dto.notification;

public enum NotificationType {
	DATA_PROVIDED("DATA_PROVIDED");

	private final String value;

	NotificationType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
