package org.folio.innreach.domain.dto.folio.sourcerecord;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsedRecordDTO {

  private UUID id;
  private String leader;
  private List<RecordFieldDTO> fields;
}
