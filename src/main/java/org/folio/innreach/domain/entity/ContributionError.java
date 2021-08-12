package org.folio.innreach.domain.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.folio.innreach.domain.entity.base.Identifiable;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"contribution"})
@Entity
@Table(name = "contribution_error")
public class ContributionError implements Identifiable<UUID> {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  private UUID recordId;

  private String message;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contribution_id", nullable = false, updatable = false)
  private Contribution contribution;

}
