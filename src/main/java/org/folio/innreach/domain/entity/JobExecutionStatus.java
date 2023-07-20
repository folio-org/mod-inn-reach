package org.folio.innreach.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "job_execution_status")
@ToString
public class JobExecutionStatus extends Auditable {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;
  private UUID instanceId;
  private UUID jobId;
  private String type;
  private String tenant;
  private boolean instanceContributed;
  private int retryAttempts;
  @Enumerated(EnumType.STRING)
  private Status status;

  public enum Status {
    IN_PROGRESS,
    READY,
    PROCESSED,
    CANCELLED,
    FAILED,
    RETRY
  }
}
