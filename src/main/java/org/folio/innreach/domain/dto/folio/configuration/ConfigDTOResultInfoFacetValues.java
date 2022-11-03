package org.folio.innreach.domain.dto.folio.configuration;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.openapitools.jackson.nullable.JsonNullable;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A facet value
 */
@ApiModel(description = "A facet value")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-10-31T21:59:20.200416+05:30[Asia/Calcutta]")

public class ConfigDTOResultInfoFacetValues   {
  @JsonProperty("count")
  private Integer count;

  @JsonProperty("value")
  private Object value;

  public ConfigDTOResultInfoFacetValues count(Integer count) {
    this.count = count;
    return this;
  }

  /**
   * Count of facet values
   * @return count
  */
  @ApiModelProperty(value = "Count of facet values")


  public Integer getCount() {
    return count;
  }

  public void setCount(Integer count) {
    this.count = count;
  }

  public ConfigDTOResultInfoFacetValues value(Object value) {
    this.value = value;
    return this;
  }

  /**
   * Value Object
   * @return value
  */
  @ApiModelProperty(value = "Value Object")

  @Valid

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConfigDTOResultInfoFacetValues configDTOResultInfoFacetValues = (ConfigDTOResultInfoFacetValues) o;
    return Objects.equals(this.count, configDTOResultInfoFacetValues.count) &&
        Objects.equals(this.value, configDTOResultInfoFacetValues.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(count, value);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConfigDTOResultInfoFacetValues {\n");
    
    sb.append("    count: ").append(toIndentedString(count)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
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

