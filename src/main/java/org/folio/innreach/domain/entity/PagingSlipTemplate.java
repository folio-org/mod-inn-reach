package org.folio.innreach.domain.entity;

import static org.folio.innreach.domain.entity.PagingSlipTemplate.FETCH_ONE_BY_CENTRAL_SERVER_ID_QUERY;
import static org.folio.innreach.domain.entity.PagingSlipTemplate.FETCH_ONE_BY_CENTRAL_SERVER_ID_QUERY_NAME;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.QueryHint;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.QueryHints;

import org.folio.innreach.domain.entity.base.Auditable;

@Getter
@Setter
@ToString(exclude = "centralServer")
@Entity
@Table(name = "paging_slip_template")
@NamedQuery(
  name = FETCH_ONE_BY_CENTRAL_SERVER_ID_QUERY_NAME,
  query = FETCH_ONE_BY_CENTRAL_SERVER_ID_QUERY,
  hints = @QueryHint(name = QueryHints.PASS_DISTINCT_THROUGH, value = "false")
)
public class PagingSlipTemplate extends Auditable {
  public static final String FETCH_ONE_BY_CENTRAL_SERVER_ID_QUERY_NAME = "PagingSlipTemplate.fetchOne";
  public static final String FETCH_ONE_BY_CENTRAL_SERVER_ID_QUERY =
    "SELECT p FROM PagingSlipTemplate AS p where p.centralServer.id = :id";

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "description")
  private String description;

  @Column(name = "template")
  private String template;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "central_server_id")
  private CentralServer centralServer;
}
