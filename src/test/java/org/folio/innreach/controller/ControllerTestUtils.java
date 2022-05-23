package org.folio.innreach.controller;

import static org.folio.innreach.util.ListUtils.mapItems;

import java.util.List;

import lombok.experimental.UtilityClass;

import org.folio.innreach.dto.ValidationErrorDTO;
import org.folio.innreach.dto.ValidationErrorsDTO;

@UtilityClass
public class ControllerTestUtils {

  static List<String> collectFieldNames(ValidationErrorsDTO errors) {
    return mapItems(errors.getValidationErrors(), ValidationErrorDTO::getFieldName);
  }

  static ValidationErrorDTO createValidationError(String field, String message) {
    var result = new ValidationErrorDTO();

    result.setFieldName(field);
    result.setMessage(message);

    return result;
  }

}
