package org.folio.innreach.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.folio.innreach.domain.entity.base.Auditable;
import org.hibernate.annotations.ColumnTransformer;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ongoing_contribution_status")
@ToString
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
  private String domainEventType;
  private String actionType;
  private JobExecutionStatus.Status status;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contribution_id", nullable = false, updatable = false)
  private Contribution contribution;
}
