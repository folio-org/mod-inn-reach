package org.folio.innreach.domain.dto.folio.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A facet
 */
@ApiModel(description = "A facet")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-10-31T21:59:20.200416+05:30[Asia/Calcutta]")

public class ConfigDTOResultInfoFacets   {
  @JsonProperty("facetValues")
  @Valid
  private List<ConfigDTOResultInfoFacetValues> facetValues = null;

  @JsonProperty("type")
  private String type;

  public ConfigDTOResultInfoFacets facetValues(List<ConfigDTOResultInfoFacetValues> facetValues) {
    this.facetValues = facetValues;
    return this;
  }

  public ConfigDTOResultInfoFacets addFacetValuesItem(ConfigDTOResultInfoFacetValues facetValuesItem) {
    if (this.facetValues == null) {
      this.facetValues = new ArrayList<>();
    }
    this.facetValues.add(facetValuesItem);
    return this;
  }

  /**
   * Array of facet values
   * @return facetValues
  */
  @ApiModelProperty(value = "Array of facet values")

  @Valid

  public List<ConfigDTOResultInfoFacetValues> getFacetValues() {
    return facetValues;
  }

  public void setFacetValues(List<ConfigDTOResultInfoFacetValues> facetValues) {
    this.facetValues = facetValues;
  }

  public ConfigDTOResultInfoFacets type(String type) {
    this.type = type;
    return this;
  }

  /**
   * Type of facet
   * @return type
  */
  @ApiModelProperty(value = "Type of facet")


  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConfigDTOResultInfoFacets configDTOResultInfoFacets = (ConfigDTOResultInfoFacets) o;
    return Objects.equals(this.facetValues, configDTOResultInfoFacets.facetValues) &&
        Objects.equals(this.type, configDTOResultInfoFacets.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(facetValues, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConfigDTOResultInfoFacets {\n");
    
    sb.append("    facetValues: ").append(toIndentedString(facetValues)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
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

