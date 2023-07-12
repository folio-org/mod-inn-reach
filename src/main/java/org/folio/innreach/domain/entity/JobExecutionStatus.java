package org.folio.innreach.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
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
  @Enumerated(EnumType.STRING)
  private Status status;

  @AllArgsConstructor
  public enum Status {
    IN_PROGRESS("In Progress"),
    READY("ready"),
    PROCESSED("Processed"),
    CANCELLED("Cancelled"),
    FAILED("Failed"),
    RETRY("Retry");

    @Getter
    private String value;
  }
}
