package org.folio.innreach.domain.entity;

import static org.hibernate.annotations.FetchMode.SUBSELECT;

import static org.folio.innreach.domain.entity.VisiblePatronFieldConfiguration.FETCH_ONE_BY_CENTRAL_CODE_QUERY;
import static org.folio.innreach.domain.entity.VisiblePatronFieldConfiguration.FETCH_ONE_BY_CENTRAL_CODE_QUERY_NAME;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Fetch;

import org.folio.innreach.domain.entity.base.Auditable;
import org.folio.innreach.domain.entity.base.Identifiable;

@Entity
@Getter
@Setter
@Table(name = "visible_patron_field_config")
@ToString(exclude = {"centralServer"})
@NamedQuery(
  name = FETCH_ONE_BY_CENTRAL_CODE_QUERY_NAME,
  query = FETCH_ONE_BY_CENTRAL_CODE_QUERY
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
    name = "visible_patron_user_custom_fields",
    joinColumns = @JoinColumn(name = "visible_patron_field_config_id")
  )
  @Column(name = "user_custom_field")
  @Fetch(value = SUBSELECT)
  private List<String> userCustomFields = new ArrayList<>();

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
