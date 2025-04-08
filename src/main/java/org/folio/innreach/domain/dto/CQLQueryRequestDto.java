package org.folio.innreach.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "offset",
    "limit",
    "query"
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CQLQueryRequestDto {

    /**
     * Skip over a number of elements by specifying an offset value for the query
     * 
     */
    @JsonProperty("offset")
    @JsonPropertyDescription("Skip over a number of elements by specifying an offset value for the query")
    @DecimalMin("0")
    @DecimalMax("2147483647")
    private Integer offset = 0;
    /**
     * Limit the number of elements returned in the response
     * 
     */
    @JsonProperty("limit")
    @JsonPropertyDescription("Limit the number of elements returned in the response")
    @DecimalMin("0")
    @DecimalMax("2147483647")
    private Integer limit = 10;
    /**
     * A query expressed as a CQL string
     * 
     */
    @JsonProperty("query")
    @JsonPropertyDescription("A query expressed as a CQL string")
    private String query;
}