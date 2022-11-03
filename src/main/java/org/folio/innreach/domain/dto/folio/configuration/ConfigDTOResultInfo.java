package org.folio.innreach.domain.dto.folio.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Faceting of result sets
 */
@ApiModel(description = "Faceting of result sets")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-10-31T21:59:20.200416+05:30[Asia/Calcutta]")

public class ConfigDTOResultInfo   {
  @JsonProperty("totalRecords")
  private Integer totalRecords;

  @JsonProperty("totalRecordsEstimated")
  private Boolean totalRecordsEstimated;

  @JsonProperty("totalRecordsRounded")
  private Integer totalRecordsRounded;

  @JsonProperty("responseTime")
  private BigDecimal responseTime;

  @JsonProperty("facets")
  @Valid
  private List<ConfigDTOResultInfoFacets> facets = null;

  @JsonProperty("diagnostics")
  @Valid
  private List<ConfigDTOResultInfoDiagnostics> diagnostics = null;

  public ConfigDTOResultInfo totalRecords(Integer totalRecords) {
    this.totalRecords = totalRecords;
    return this;
  }

  /**
   * Estimated or exact total number of records
   * @return totalRecords
  */
  @ApiModelProperty(value = "Estimated or exact total number of records")


  public Integer getTotalRecords() {
    return totalRecords;
  }

  public void setTotalRecords(Integer totalRecords) {
    this.totalRecords = totalRecords;
  }

  public ConfigDTOResultInfo totalRecordsEstimated(Boolean totalRecordsEstimated) {
    this.totalRecordsEstimated = totalRecordsEstimated;
    return this;
  }

  /**
   * True if totalRecords is an estimation, false if it is the exact number
   * @return totalRecordsEstimated
  */
  @ApiModelProperty(value = "True if totalRecords is an estimation, false if it is the exact number")


  public Boolean getTotalRecordsEstimated() {
    return totalRecordsEstimated;
  }

  public void setTotalRecordsEstimated(Boolean totalRecordsEstimated) {
    this.totalRecordsEstimated = totalRecordsEstimated;
  }

  public ConfigDTOResultInfo totalRecordsRounded(Integer totalRecordsRounded) {
    this.totalRecordsRounded = totalRecordsRounded;
    return this;
  }

  /**
   * The rounded value of totalRecords if totalRecords is an estimation
   * @return totalRecordsRounded
  */
  @ApiModelProperty(value = "The rounded value of totalRecords if totalRecords is an estimation")


  public Integer getTotalRecordsRounded() {
    return totalRecordsRounded;
  }

  public void setTotalRecordsRounded(Integer totalRecordsRounded) {
    this.totalRecordsRounded = totalRecordsRounded;
  }

  public ConfigDTOResultInfo responseTime(BigDecimal responseTime) {
    this.responseTime = responseTime;
    return this;
  }

  /**
   * Response time
   * @return responseTime
  */
  @ApiModelProperty(value = "Response time")

  @Valid

  public BigDecimal getResponseTime() {
    return responseTime;
  }

  public void setResponseTime(BigDecimal responseTime) {
    this.responseTime = responseTime;
  }

  public ConfigDTOResultInfo facets(List<ConfigDTOResultInfoFacets> facets) {
    this.facets = facets;
    return this;
  }

  public ConfigDTOResultInfo addFacetsItem(ConfigDTOResultInfoFacets facetsItem) {
    if (this.facets == null) {
      this.facets = new ArrayList<>();
    }
    this.facets.add(facetsItem);
    return this;
  }

  /**
   * Array of facets
   * @return facets
  */
  @ApiModelProperty(value = "Array of facets")

  @Valid

  public List<ConfigDTOResultInfoFacets> getFacets() {
    return facets;
  }

  public void setFacets(List<ConfigDTOResultInfoFacets> facets) {
    this.facets = facets;
  }

  public ConfigDTOResultInfo diagnostics(List<ConfigDTOResultInfoDiagnostics> diagnostics) {
    this.diagnostics = diagnostics;
    return this;
  }

  public ConfigDTOResultInfo addDiagnosticsItem(ConfigDTOResultInfoDiagnostics diagnosticsItem) {
    if (this.diagnostics == null) {
      this.diagnostics = new ArrayList<>();
    }
    this.diagnostics.add(diagnosticsItem);
    return this;
  }

  /**
   * Array of diagnostic information
   * @return diagnostics
  */
  @ApiModelProperty(value = "Array of diagnostic information")

  @Valid

  public List<ConfigDTOResultInfoDiagnostics> getDiagnostics() {
    return diagnostics;
  }

  public void setDiagnostics(List<ConfigDTOResultInfoDiagnostics> diagnostics) {
    this.diagnostics = diagnostics;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConfigDTOResultInfo configDTOResultInfo = (ConfigDTOResultInfo) o;
    return Objects.equals(this.totalRecords, configDTOResultInfo.totalRecords) &&
        Objects.equals(this.totalRecordsEstimated, configDTOResultInfo.totalRecordsEstimated) &&
        Objects.equals(this.totalRecordsRounded, configDTOResultInfo.totalRecordsRounded) &&
        Objects.equals(this.responseTime, configDTOResultInfo.responseTime) &&
        Objects.equals(this.facets, configDTOResultInfo.facets) &&
        Objects.equals(this.diagnostics, configDTOResultInfo.diagnostics);
  }

  @Override
  public int hashCode() {
    return Objects.hash(totalRecords, totalRecordsEstimated, totalRecordsRounded, responseTime, facets, diagnostics);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConfigDTOResultInfo {\n");
    
    sb.append("    totalRecords: ").append(toIndentedString(totalRecords)).append("\n");
    sb.append("    totalRecordsEstimated: ").append(toIndentedString(totalRecordsEstimated)).append("\n");
    sb.append("    totalRecordsRounded: ").append(toIndentedString(totalRecordsRounded)).append("\n");
    sb.append("    responseTime: ").append(toIndentedString(responseTime)).append("\n");
    sb.append("    facets: ").append(toIndentedString(facets)).append("\n");
    sb.append("    diagnostics: ").append(toIndentedString(diagnostics)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

