package org.folio.innreach.domain.dto.folio.sourcerecord;

import java.util.UUID;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.folio.innreach.domain.dto.folio.sourcerecord.deserializer.SourceRecordDTODeserializer;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonDeserialize(using = SourceRecordDTODeserializer.class)
public class SourceRecordDTO {

  private UUID id;
  private String recordType;
  private String state;
  private boolean deleted;
  private ParsedRecordDTO parsedRecord;
}
