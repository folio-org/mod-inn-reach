package org.folio.innreach.domain.entity;

import static org.hibernate.annotations.FetchMode.SUBSELECT;

import static org.folio.innreach.domain.entity.VisiblePatronFieldConfiguration.FETCH_ONE_BY_CENTRAL_CODE_QUERY;
import static org.folio.innreach.domain.entity.VisiblePatronFieldConfiguration.FETCH_ONE_BY_CENTRAL_CODE_QUERY_NAME;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.QueryHints;

import org.folio.innreach.domain.entity.base.Auditable;
import org.folio.innreach.domain.entity.base.Identifiable;

@Entity
@Getter
@Setter
@Table(name = "visible_patron_field_config")
@ToString(exclude = {"centralServer"})
@NamedQuery(
  name = FETCH_ONE_BY_CENTRAL_CODE_QUERY_NAME,
  query = FETCH_ONE_BY_CENTRAL_CODE_QUERY,
  hints = @QueryHint(name = QueryHints.PASS_DISTINCT_THROUGH, value = "false")
)
public class VisiblePatronFieldConfiguration extends Auditable implements Identifiable<UUID> {
  public static final String FETCH_ONE_BY_CENTRAL_CODE_QUERY_NAME = "VisiblePatronFieldConfiguration.fetchOneByCode";
  public static final String FETCH_ONE_BY_CENTRAL_CODE_QUERY = "SELECT config FROM VisiblePatronFieldConfiguration as config " +
    "WHERE config.centralServer.centralServerCode = :centralServerCode";

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
    name = "visible_patron_fields",
    joinColumns = @JoinColumn(name = "visible_patron_field_config_id")
  )
  @Column(name = "visible_patron_field")
  @Fetch(value = SUBSELECT)
  private Set<VisiblePatronField> fields = new LinkedHashSet<>();

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
    name = "visible_patron_user_custom_field_id",
    joinColumns = @JoinColumn(name = "visible_patron_field_config_id")
  )
  @Column(name = "user_custom_field_id")
  @Fetch(value = SUBSELECT)
  private List<UUID> userCustomFieldIds = new ArrayList<>();

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "central_server_id")
  private CentralServer centralServer;

  @AllArgsConstructor
  public enum VisiblePatronField {
    BARCODE("barcode"),
    EXTERNAL_SYSTEM_ID("externalSystemId"),
    FOLIO_RECORD_NUMBER("folioRecordNumber"),
    USERNAME("username"),
    USER_CUSTOM_FIELDS("userCustomFields");

    @Getter
    private final String value;
  }
}
