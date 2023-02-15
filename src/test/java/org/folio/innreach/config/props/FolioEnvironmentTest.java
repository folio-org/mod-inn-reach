package org.folio.innreach.config.props;

import static org.assertj.core.api.Assertions.assertThat;

import static org.folio.innreach.config.props.FolioEnvironment.getFolioEnvName;
import static org.folio.innreach.fixture.TestUtil.removeEnvProperty;
import static org.folio.innreach.fixture.TestUtil.setEnvProperty;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FolioEnvironmentTest {

  @AfterEach
  void resetEnvPropertyValue() {
    removeEnvProperty();
  }

  @Test
  void shouldReturnFolioEnvFromProperties() {
    setEnvProperty("test-env");
    assertThat(getFolioEnvName()).isEqualTo("test-env");
  }

  @Test
  void shouldReturnDefaultFolioEnvIfPropertyNotSet() {
    assertThat(getFolioEnvName()).isEqualTo("folio");
  }

  @Test
  void shouldReturnDefaultFolioEnvIfPropertyIsEmpty() {
    setEnvProperty("   ");
    assertThat(getFolioEnvName()).isEqualTo("folio");
  }

  @ValueSource(strings = {"a", "Z", "0", "9", "_", "-"})
  @ParameterizedTest
  void shouldNotThrowExceptionWhenEnvHasAllowedChars(String env) {
    setEnvProperty(env);
    assertThat(getFolioEnvName()).isEqualTo(env);
  }

  @ParameterizedTest
  @ValueSource(strings = {"!", "@", "%$$#", "def qa"})
  void shouldThrowExceptionWhenEnvHasDisallowedChars(String env) {
    var validator = Validation.buildDefaultValidatorFactory().getValidator();
    var folioEnvironment = FolioEnvironment.of(env);
    var validationResponse = validator.validate(folioEnvironment);
    assertThat(validationResponse).isNotEmpty()
      .map(ConstraintViolation::getMessage)
      .containsExactly("Value must follow the pattern: '[\\w0-9\\-_]+'");
  }
}
