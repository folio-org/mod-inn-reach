package org.folio.innreach.domain.dto.folio.configuration;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.folio.innreach.dto.MetadataSchema;
import org.openapitools.jackson.nullable.JsonNullable;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ConfigDTOConfigs
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-10-31T21:59:20.200416+05:30[Asia/Calcutta]")

public class ConfigDTOConfigs   {
  @JsonProperty("id")
  private String id;

  @JsonProperty("module")
  private String module;

  @JsonProperty("configName")
  private String configName;

  @JsonProperty("code")
  private String code;

  @JsonProperty("description")
  private String description;

  @JsonProperty("default")
  private Boolean _default;

  @JsonProperty("enabled")
  private Boolean enabled;

  @JsonProperty("value")
  private String value;

  @JsonProperty("userId")
  private String userId;

  @JsonProperty("metadata")
  private MetadataSchema metadata;

  public ConfigDTOConfigs id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
  */
  @ApiModelProperty(value = "")

@Pattern(regexp="^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$") 
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public ConfigDTOConfigs module(String module) {
    this.module = module;
    return this;
  }

  /**
   * Get module
   * @return module
  */
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getModule() {
    return module;
  }

  public void setModule(String module) {
    this.module = module;
  }

  public ConfigDTOConfigs configName(String configName) {
    this.configName = configName;
    return this;
  }

  /**
   * Get configName
   * @return configName
  */
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getConfigName() {
    return configName;
  }

  public void setConfigName(String configName) {
    this.configName = configName;
  }

  public ConfigDTOConfigs code(String code) {
    this.code = code;
    return this;
  }

  /**
   * Get code
   * @return code
  */
  @ApiModelProperty(value = "")


  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public ConfigDTOConfigs description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
  */
  @ApiModelProperty(value = "")


  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ConfigDTOConfigs _default(Boolean _default) {
    this._default = _default;
    return this;
  }

  /**
   * Get _default
   * @return _default
  */
  @ApiModelProperty(value = "")


  public Boolean getDefault() {
    return _default;
  }

  public void setDefault(Boolean _default) {
    this._default = _default;
  }

  public ConfigDTOConfigs enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * Get enabled
   * @return enabled
  */
  @ApiModelProperty(value = "")


  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public ConfigDTOConfigs value(String value) {
    this.value = value;
    return this;
  }

  /**
   * Get value
   * @return value
  */
  @ApiModelProperty(value = "")


  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public ConfigDTOConfigs userId(String userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Get userId
   * @return userId
  */
  @ApiModelProperty(value = "")


  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public ConfigDTOConfigs metadata(MetadataSchema metadata) {
    this.metadata = metadata;
    return this;
  }

  /**
   * Get metadata
   * @return metadata
  */
  @ApiModelProperty(value = "")

  @Valid

  public MetadataSchema getMetadata() {
    return metadata;
  }

  public void setMetadata(MetadataSchema metadata) {
    this.metadata = metadata;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConfigDTOConfigs configDTOConfigs = (ConfigDTOConfigs) o;
    return Objects.equals(this.id, configDTOConfigs.id) &&
        Objects.equals(this.module, configDTOConfigs.module) &&
        Objects.equals(this.configName, configDTOConfigs.configName) &&
        Objects.equals(this.code, configDTOConfigs.code) &&
        Objects.equals(this.description, configDTOConfigs.description) &&
        Objects.equals(this._default, configDTOConfigs._default) &&
        Objects.equals(this.enabled, configDTOConfigs.enabled) &&
        Objects.equals(this.value, configDTOConfigs.value) &&
        Objects.equals(this.userId, configDTOConfigs.userId) &&
        Objects.equals(this.metadata, configDTOConfigs.metadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, module, configName, code, description, _default, enabled, value, userId, metadata);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConfigDTOConfigs {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    module: ").append(toIndentedString(module)).append("\n");
    sb.append("    configName: ").append(toIndentedString(configName)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    _default: ").append(toIndentedString(_default)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    metadata: ").append(toIndentedString(metadata)).append("\n");
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

