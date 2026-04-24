package it.eng.onenet.dsp.api.dto.connector;

import lombok.Data;

@Data
public class OuterResponse<T> {
  private ConnectorResponse<T> response;
}
