package org.folio.innreach.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.folio.innreach.domain.entity.base.Auditable;
import org.folio.innreach.domain.event.DomainEventType;
import org.hibernate.annotations.ColumnTransformer;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ongoing_contribution_status")
@EqualsAndHashCode(of = "id", callSuper = false)
public class OngoingContributionStatus extends Auditable {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;
  @ColumnTransformer(write = "?::jsonb")
  @Column(columnDefinition = "jsonb")
  private String oldEntity;
  @ColumnTransformer(write = "?::jsonb")
  @Column(columnDefinition = "jsonb")
  private String newEntity;
  @Enumerated(EnumType.STRING)
  private EventName domainEventName;
  @Enumerated(EnumType.STRING)
  private DomainEventType domainEventType;
  private UUID centralServerId;
  @Enumerated(EnumType.STRING)
  private ContributionStatus status;
  private int retryAttempts;
  private String error;
  private String tenant;
  public enum EventName {
    ITEM,
    INSTANCE,
    HOLDINGS
  }
}
