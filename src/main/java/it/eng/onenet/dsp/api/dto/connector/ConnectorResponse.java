package it.eng.onenet.dsp.api.dto.connector;

import lombok.Data;

@Data
public class ConnectorResponse<T> {
    private boolean success;
    private String message;
    T data;
    private String timestamp;
}
