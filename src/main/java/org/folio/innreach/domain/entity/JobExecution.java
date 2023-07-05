package org.folio.innreach.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.folio.innreach.domain.entity.base.Auditable;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "job_executions")
@ToString
public class JobExecution extends Auditable {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;
  private Integer totalRecords;
  private boolean isFinished;
}
