package it.eng.onenet.dsp.api.controller.consume_data;

import it.eng.onenet.dsp.api.dto.FileResponse;
import it.eng.onenet.dsp.api.dto.list_results.ListResultsDataDTO;
import it.eng.onenet.dsp.api.service.consume_data.ConsumeDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/consume-data")
@Slf4j
@Tag(name = "Consume Data", description = "These endpoints provide the data to be consumed (All of them or by {page}). In addition, you can retrieve a specific file or its metadata by its {fileid}.")
public class ConsumeDataController {

    private final ConsumeDataService consumeDataService;

    public ConsumeDataController(ConsumeDataService consumeDataService) {
        this.consumeDataService = consumeDataService;
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("list")
    public List<Map<String, Object>> getList(@RequestHeader Map<String, String> headers) {
        return consumeDataService.getList(headers);
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("page/{page}")
    public ListResultsDataDTO getPage(
            @RequestHeader Map<String, String> headers,
            @PathVariable("page") Long page) {
        return consumeDataService.getPage(headers, page);
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("metadata/{fileid}")
    public Map<String, Object> getEntity(@RequestHeader Map<String, String> headers, @PathVariable("fileid") String id) {
      return consumeDataService.getEntity(headers, id);
    }

    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("{fileid}")
    public FileResponse getFile(@RequestHeader Map<String, String> headers, @PathVariable("fileid") String id) throws IOException {
      return consumeDataService.getFile(headers, id);
    }

}
