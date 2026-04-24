package it.eng.onenet.dsp.api.dto.connector;

import java.util.List;

import lombok.Data;

@Data
public class PagedResponse<T> {
  private List<T> content;
  private List<Link> links;
  private PageMetadata page;

  @Data
  public static class Link {
    private String rel;
    private String href;
  }

  @Data
  public static class PageMetadata {
    private int size;
    private int totalElements;
    private int totalPages;
    private int number;
  }
}
