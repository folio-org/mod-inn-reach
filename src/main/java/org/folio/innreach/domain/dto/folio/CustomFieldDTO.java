package org.folio.innreach.domain.dto.folio;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class CustomFieldDTO {
  private UUID id;
  private String refId;
  private FieldType type;

  public enum FieldType {
    RADIO_BUTTON, SINGLE_CHECKBOX, SINGLE_SELECT_DROPDOWN, MULTI_SELECT_DROPDOWN, TEXTBOX_SHORT, TEXTBOX_LONG
  }
}
