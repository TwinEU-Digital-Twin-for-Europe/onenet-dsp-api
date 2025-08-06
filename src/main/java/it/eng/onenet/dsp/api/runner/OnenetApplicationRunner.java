package it.eng.onenet.dsp.api.runner;

import java.io.IOException;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class OnenetApplicationRunner implements ApplicationRunner {
    private final InitDspConnectorService initDspConnectorService;

    public OnenetApplicationRunner(InitDspConnectorService initDspConnectorService) {
        this.initDspConnectorService = initDspConnectorService;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException, InterruptedException {
        initDspConnectorService.init();
    }

}
