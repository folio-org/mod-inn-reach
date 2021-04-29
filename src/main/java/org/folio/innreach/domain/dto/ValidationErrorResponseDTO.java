package org.folio.innreach.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ValidationErrorResponseDTO {
  private Date timestamp;
  private int status;
  private String message;
  private Set<String> validationErrorMessages;
}
