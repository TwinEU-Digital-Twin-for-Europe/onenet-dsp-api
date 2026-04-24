package it.eng.onenet.dsp.api.controller.health;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/health")
@Slf4j
@Tag(name = "Health Check")
public class HealthController {
  @GetMapping
  public String getObject() {
    String version = "unknown";
    log.info("Health Check - Attempting to read project version from pom.xml");
    try {
      MavenXpp3Reader reader = new MavenXpp3Reader();
      Model model = reader.read(new FileReader("pom.xml"));
      version = model.getVersion();
      if (version != null && !version.isEmpty())
        log.info("Project Version: [{}].", version);
    } catch (Exception e) {
      log.info("Failed to read version.");
    }
    return String.format("{\"version\":\"%s\"}", version);
  }
}