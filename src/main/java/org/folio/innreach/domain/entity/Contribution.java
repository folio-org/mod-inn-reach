package org.folio.innreach.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.folio.innreach.domain.entity.base.Auditable;
import org.folio.innreach.domain.entity.base.Identifiable;
import org.hibernate.annotations.QueryHints;

import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.folio.innreach.domain.entity.Contribution.FETCH_CURRENT_QUERY;
import static org.folio.innreach.domain.entity.Contribution.FETCH_CURRENT_QUERY_NAME;
import static org.folio.innreach.domain.entity.Contribution.FETCH_HISTORY_COUNT_QUERY_NAME;
import static org.folio.innreach.domain.entity.Contribution.FETCH_HISTORY_QUERY;
import static org.folio.innreach.domain.entity.Contribution.FETCH_HISTORY_QUERY_NAME;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"centralServer", "errors"})
@Entity
@NamedQuery(
  name = FETCH_HISTORY_QUERY_NAME,
  query = FETCH_HISTORY_QUERY,
  hints = @QueryHint(name = QueryHints.PASS_DISTINCT_THROUGH, value = "false")
)
@NamedQuery(name = FETCH_HISTORY_COUNT_QUERY_NAME, query = Contribution.FETCH_HISTORY_COUNT_QUERY)
@NamedQuery(
  name = FETCH_CURRENT_QUERY_NAME,
  query = FETCH_CURRENT_QUERY,
  hints = @QueryHint(name = QueryHints.PASS_DISTINCT_THROUGH, value = "false")
)
@Table(name = "contribution")
public class Contribution extends Auditable<String> implements Identifiable<UUID> {

  private static final String FETCH_CURRENT_POSTFIX = " WHERE c.centralServer.id = :id AND c.status = 0";
  public static final String FETCH_CURRENT_QUERY_NAME = "Contribution.fetchCurrent";
  public static final String FETCH_CURRENT_QUERY = "SELECT DISTINCT c FROM Contribution AS c " +
    "LEFT JOIN FETCH c.errors" + FETCH_CURRENT_POSTFIX;

  private static final String FETCH_HISTORY_POSTFIX = " WHERE c.centralServer.id = :id AND c.status != 0";
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

  @OneToMany(mappedBy = "contribution", fetch = FetchType.LAZY)
  private List<ContributionError> errors = new ArrayList<>();

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "central_server_id", unique = true, nullable = false, updatable = false)
  private CentralServer centralServer;

  @AllArgsConstructor
  public enum Status {
    IN_PROGRESS("In Progress"),
    COMPLETE("Complete");

    @Getter
    private String value;
  }

}
