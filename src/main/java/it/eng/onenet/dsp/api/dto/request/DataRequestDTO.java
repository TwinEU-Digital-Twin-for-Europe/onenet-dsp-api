package it.eng.onenet.dsp.api.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataRequestDTO {

	@JsonProperty("cf_comments")
	private String cfComments;

	@JsonProperty("company_id")
	private UUID companyId;

	private String title;

	@JsonProperty("user_requesting")
	private String userRequesting;

	@JsonProperty("user_requesting_id")
	private UUID userRequestingId;

	@JsonProperty("data_catalog_category_id")
	private UUID dataCatalogCategoryId;

	@JsonProperty("created_on")
	@JsonFormat(pattern = "dd/MM/yyyy HH:mm")
	private LocalDateTime createdOn;

	@JsonProperty("cf_viewed_by_offering_owner")
	private String cfViewedByOfferingOwner;

	@JsonProperty("data_offering_id")
	private UUID dataOfferingId;

	@JsonProperty("user_offering_id")
	private UUID userOfferingId;

	@JsonProperty("user_offering")
	private String userOffering;

	private String category;

	@JsonProperty("request_id")
	private UUID requestId;

	@JsonProperty("cf_type")
	private String cfType;

	private String status;

}
