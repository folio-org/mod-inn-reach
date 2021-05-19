package org.folio.innreach.domain.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.time.LocalDateTime;
import java.util.UUID;

@Embeddable

@NoArgsConstructor
@Data
public class MetaData {
  @Column(name = "created_by_user_id")
  UUID createdByUserId;

  @Column(name = "created_date")
  LocalDateTime createdDate;

  @Column(name = "updated_by_user_id")
  UUID updatedByUserId;

  @Column(name = "updated_date")
  LocalDateTime updatedDate;
}
