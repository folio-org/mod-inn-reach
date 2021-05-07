package org.folio.innreach.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ErrorResponseDTO {
  private Date timestamp;
  private int status;
  private String message;
}
