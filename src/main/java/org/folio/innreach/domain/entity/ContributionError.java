package org.folio.innreach.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.folio.innreach.domain.entity.base.Identifiable;

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
