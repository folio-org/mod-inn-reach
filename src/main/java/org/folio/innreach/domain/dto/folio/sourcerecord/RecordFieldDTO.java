package org.folio.innreach.domain.dto.folio.sourcerecord;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "code")
@Builder
public class RecordFieldDTO {

  private String code;
  private String value;
  private List<SubFieldDTO> subFields;
  private Character ind1;
  private Character ind2;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(of = "code")
  public static class SubFieldDTO {
    private Character code;
    private String value;
  }
}
