package org.folio.innreach.domain.dto.folio.inventory;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonDeserialize(using = SourceRecordDTODeserializer.class)
public class SourceRecordDTO {

  private UUID id;
  private String recordType;
  private String state;
  private boolean deleted;
  private List<RecordField> fields;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(of = "code")
  public static class RecordField {

    private String code;
    private String value;
    private List<RecordField> subFields;
  }
}
