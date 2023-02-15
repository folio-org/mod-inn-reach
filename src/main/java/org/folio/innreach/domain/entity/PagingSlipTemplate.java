package org.folio.innreach.domain.entity;

import static org.folio.innreach.domain.entity.PagingSlipTemplate.FETCH_ONE_BY_CENTRAL_SERVER_ID_QUERY;
import static org.folio.innreach.domain.entity.PagingSlipTemplate.FETCH_ONE_BY_CENTRAL_SERVER_ID_QUERY_NAME;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.folio.innreach.domain.entity.base.Auditable;

@Getter
@Setter
@ToString(exclude = "centralServer")
@Entity
@Table(name = "paging_slip_template")
@NamedQuery(
  name = FETCH_ONE_BY_CENTRAL_SERVER_ID_QUERY_NAME,
  query = FETCH_ONE_BY_CENTRAL_SERVER_ID_QUERY
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
