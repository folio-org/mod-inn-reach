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
 * Diagnostic information
 */
@ApiModel(description = "Diagnostic information")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-10-31T21:59:20.200416+05:30[Asia/Calcutta]")

public class ConfigDTOResultInfoDiagnostics   {
  @JsonProperty("source")
  private String source;

  @JsonProperty("code")
  private String code;

  @JsonProperty("message")
  private String message;

  @JsonProperty("module")
  private String module;

  @JsonProperty("recordCount")
  private Integer recordCount;

  @JsonProperty("query")
  private String query;

  public ConfigDTOResultInfoDiagnostics source(String source) {
    this.source = source;
    return this;
  }

  /**
   * Source reporting the diagnostic information
   * @return source
  */
  @ApiModelProperty(value = "Source reporting the diagnostic information")


  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public ConfigDTOResultInfoDiagnostics code(String code) {
    this.code = code;
    return this;
  }

  /**
   * Diagnostic Code
   * @return code
  */
  @ApiModelProperty(value = "Diagnostic Code")


  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public ConfigDTOResultInfoDiagnostics message(String message) {
    this.message = message;
    return this;
  }

  /**
   * Diagnostic Message
   * @return message
  */
  @ApiModelProperty(value = "Diagnostic Message")


  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public ConfigDTOResultInfoDiagnostics module(String module) {
    this.module = module;
    return this;
  }

  /**
   * Module reporting diagnostic information
   * @return module
  */
  @ApiModelProperty(value = "Module reporting diagnostic information")


  public String getModule() {
    return module;
  }

  public void setModule(String module) {
    this.module = module;
  }

  public ConfigDTOResultInfoDiagnostics recordCount(Integer recordCount) {
    this.recordCount = recordCount;
    return this;
  }

  /**
   * Record Count for diagnostics
   * @return recordCount
  */
  @ApiModelProperty(value = "Record Count for diagnostics")


  public Integer getRecordCount() {
    return recordCount;
  }

  public void setRecordCount(Integer recordCount) {
    this.recordCount = recordCount;
  }

  public ConfigDTOResultInfoDiagnostics query(String query) {
    this.query = query;
    return this;
  }

  /**
   * CQL Query associated with results
   * @return query
  */
  @ApiModelProperty(value = "CQL Query associated with results")


  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConfigDTOResultInfoDiagnostics configDTOResultInfoDiagnostics = (ConfigDTOResultInfoDiagnostics) o;
    return Objects.equals(this.source, configDTOResultInfoDiagnostics.source) &&
        Objects.equals(this.code, configDTOResultInfoDiagnostics.code) &&
        Objects.equals(this.message, configDTOResultInfoDiagnostics.message) &&
        Objects.equals(this.module, configDTOResultInfoDiagnostics.module) &&
        Objects.equals(this.recordCount, configDTOResultInfoDiagnostics.recordCount) &&
        Objects.equals(this.query, configDTOResultInfoDiagnostics.query);
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, code, message, module, recordCount, query);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConfigDTOResultInfoDiagnostics {\n");
    
    sb.append("    source: ").append(toIndentedString(source)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    module: ").append(toIndentedString(module)).append("\n");
    sb.append("    recordCount: ").append(toIndentedString(recordCount)).append("\n");
    sb.append("    query: ").append(toIndentedString(query)).append("\n");
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

