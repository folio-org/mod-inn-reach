package org.folio.innreach.domain.dto.folio.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ConfigDTO
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-10-31T21:59:20.200416+05:30[Asia/Calcutta]")

public class ConfigDTO   {
  @JsonProperty("configs")
  @Valid
  private List<ConfigDTOConfigs> configs = new ArrayList<>();

  @JsonProperty("totalRecords")
  private Integer totalRecords;

  @JsonProperty("resultInfo")
  private ConfigDTOResultInfo resultInfo;

  public ConfigDTO configs(List<ConfigDTOConfigs> configs) {
    this.configs = configs;
    return this;
  }

  public ConfigDTO addConfigsItem(ConfigDTOConfigs configsItem) {
    this.configs.add(configsItem);
    return this;
  }

  /**
   * Get configs
   * @return configs
  */
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public List<ConfigDTOConfigs> getConfigs() {
    return configs;
  }

  public void setConfigs(List<ConfigDTOConfigs> configs) {
    this.configs = configs;
  }

  public ConfigDTO totalRecords(Integer totalRecords) {
    this.totalRecords = totalRecords;
    return this;
  }

  /**
   * Get totalRecords
   * @return totalRecords
  */
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Integer getTotalRecords() {
    return totalRecords;
  }

  public void setTotalRecords(Integer totalRecords) {
    this.totalRecords = totalRecords;
  }

  public ConfigDTO resultInfo(ConfigDTOResultInfo resultInfo) {
    this.resultInfo = resultInfo;
    return this;
  }

  /**
   * Get resultInfo
   * @return resultInfo
  */
  @ApiModelProperty(value = "")

  @Valid

  public ConfigDTOResultInfo getResultInfo() {
    return resultInfo;
  }

  public void setResultInfo(ConfigDTOResultInfo resultInfo) {
    this.resultInfo = resultInfo;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConfigDTO configDTO = (ConfigDTO) o;
    return Objects.equals(this.configs, configDTO.configs) &&
        Objects.equals(this.totalRecords, configDTO.totalRecords) &&
        Objects.equals(this.resultInfo, configDTO.resultInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(configs, totalRecords, resultInfo);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConfigDTO {\n");
    
    sb.append("    configs: ").append(toIndentedString(configs)).append("\n");
    sb.append("    totalRecords: ").append(toIndentedString(totalRecords)).append("\n");
    sb.append("    resultInfo: ").append(toIndentedString(resultInfo)).append("\n");
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

