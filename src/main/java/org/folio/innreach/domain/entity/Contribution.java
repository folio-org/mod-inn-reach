package org.folio.innreach.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.folio.innreach.domain.entity.base.Auditable;
import org.folio.innreach.domain.entity.base.Identifiable;

import static org.folio.innreach.domain.entity.Contribution.*;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"centralServer", "errors"})
@Entity
@NamedQuery(
  name = FETCH_HISTORY_QUERY_NAME,
  query = FETCH_HISTORY_QUERY
)
@NamedQuery(name = FETCH_HISTORY_COUNT_QUERY_NAME, query = Contribution.FETCH_HISTORY_COUNT_QUERY)
@NamedQuery(
  name = FETCH_CURRENT_QUERY_NAME,
  query = FETCH_CURRENT_QUERY
)
@NamedQuery(
  name = FETCH_ONGOING_QUERY_NAME,
  query = FETCH_ONGOING_QUERY
)
@Table(name = "contribution")
public class Contribution extends Auditable implements Identifiable<UUID> {

  private static final String FETCH_CURRENT_POSTFIX = " WHERE c.centralServer.id = :id AND c.status = 0 AND c.ongoing = FALSE";
  private static final String FETCH_ONGOING_POSTFIX = " WHERE c.centralServer.id = :id AND c.status = 0 AND c.ongoing = TRUE";
  public static final String FETCH_CURRENT_QUERY_NAME = "Contribution.fetchCurrent";
  public static final String FETCH_ONGOING_QUERY_NAME = "Contribution.fetchOngoing";
  public static final String FETCH_CURRENT_QUERY = "SELECT DISTINCT c FROM Contribution AS c " +
    "LEFT JOIN FETCH c.errors" + FETCH_CURRENT_POSTFIX;
  public static final String FETCH_ONGOING_QUERY = "SELECT DISTINCT c FROM Contribution AS c " +
    "LEFT JOIN FETCH c.errors" + FETCH_ONGOING_POSTFIX;

  private static final String FETCH_HISTORY_POSTFIX = " WHERE c.centralServer.id = :id AND c.status != 0 AND c.ongoing = FALSE";
  public static final String FETCH_HISTORY_QUERY_NAME = "Contribution.fetchHistory";
  public static final String FETCH_HISTORY_QUERY = "SELECT DISTINCT c FROM Contribution AS c " +
    "LEFT JOIN FETCH c.errors" + FETCH_HISTORY_POSTFIX;

  public static final String FETCH_HISTORY_COUNT_QUERY_NAME = "Contribution.fetchHistoryCount";
  public static final String FETCH_HISTORY_COUNT_QUERY = "SELECT count(c) FROM Contribution c" + FETCH_HISTORY_POSTFIX;

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Enumerated
  private Status status;

  private OffsetDateTime completeDate;

  private Long recordsTotal;

  private Long recordsProcessed;

  private Long recordsContributed;

  private Long recordsUpdated;

  private Long recordsDecontributed;

  private boolean ongoing;

  @OneToMany(mappedBy = "contribution", fetch = FetchType.LAZY)
  private List<ContributionError> errors = new ArrayList<>();

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "central_server_id", unique = true, nullable = false, updatable = false)
  private CentralServer centralServer;

  @Column(name = "job_id")
  private UUID jobId;

  @AllArgsConstructor
  public enum Status {
    IN_PROGRESS("In Progress"),
    COMPLETE("Complete"),
    NOT_STARTED("Not started"),
    CANCELLED("Cancelled");

    @Getter
    private String value;
  }

}
