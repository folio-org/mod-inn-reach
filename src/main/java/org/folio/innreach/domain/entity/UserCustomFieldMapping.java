package org.folio.innreach.domain.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.folio.innreach.domain.entity.base.Auditable;
import org.folio.innreach.domain.entity.base.Identifiable;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Getter
@Setter
@Table(name = "user_custom_field_mapping")
@ToString(exclude = {"centralServer"})
public class UserCustomFieldMapping extends Auditable implements Identifiable<UUID> {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "custom_field_id")
  private String customFieldId;

  @ElementCollection(fetch = FetchType.LAZY)
  @JoinTable(name = "user_custom_field_configured_options", joinColumns = @JoinColumn(name = "user_custom_field_mapping_id"))
  @MapKeyColumn(name = "custom_field_value")
  @Column(name = "agency_code")
  @Fetch(value = FetchMode.JOIN)
  private Map<String, String> configuredOptions = new HashMap<>();

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "central_server_id")
  private CentralServer centralServer;
}
