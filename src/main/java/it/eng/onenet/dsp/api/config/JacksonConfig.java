package it.eng.onenet.dsp.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class JacksonConfig {

  @Value("${jackson.stream.max-string-length}")
  private int maxStringLength;

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.getFactory().setStreamReadConstraints(
        StreamReadConstraints.builder()
            .maxStringLength(maxStringLength)
            .build());
    return objectMapper;
  }

}
